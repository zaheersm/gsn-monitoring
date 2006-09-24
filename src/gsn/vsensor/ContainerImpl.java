package gsn.vsensor ;

import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.notifications.GSNNotification;
import gsn.notifications.NotificationRequest;
import gsn.storage.StorageManager;
import gsn.utils.CaseInsensitiveComparator;
import gsn.wrappers.RemoteDS;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * @web.servlet name="gsn" load-on-startup="true"
 * @web.servlet-mapping url-pattern="/gsn"
 */
public class ContainerImpl extends HttpServlet implements Container {

    private static transient Logger logger = Logger.getLogger(ContainerImpl.class);

    /**
     * The <code> waitingVirtualSensors</code> contains the virtual sensors that
     * recently produced data. This variable is useful for batch processing TIMED
     * couple virtual sensor produce data.
     */
    /*
    * In the <code>registeredQueries</code> the key is the local virtual
    * sensor name.
    */
    private static TreeMap<String, ArrayList<NotificationRequest>> notificationRequests = null;

    private static final Class<ContainerImpl> notificationRequestsLock = ContainerImpl.class;

    private static final HashMap<String, RemoteDS> notificationCodeToRemoteDataSource = new HashMap<String, RemoteDS>();

    private static final Object psLock = new Object();

    public ContainerImpl() {
        notificationRequests = new TreeMap<String, ArrayList<NotificationRequest>>(new CaseInsensitiveComparator());
    }

    public void publishData(VirtualSensor sensor) {
        StreamElement data = sensor.getData();
        String name = sensor.getName();
        StorageManager storageMan = StorageManager.getInstance();
        synchronized (psLock) {
            storageMan.insertDataNoDupError(name, data);
        }
        // SimulationResult.addJustBeforeStartingToEvaluateQueries ();
        ArrayList<NotificationRequest> registered;
        synchronized (notificationRequestsLock) {
            registered = notificationRequests.get(name);
        }
        if (registered == null) {
            if (logger.isDebugEnabled())
                logger.debug(new StringBuilder().append("No Query registered for >").append(name).append("<").toString());
            // SimulationResult.addJustQueryEvaluationFinished ( 0 );
            return;
        }
        if (logger.isDebugEnabled())
            logger.debug(new StringBuilder()
                    .append("There are queries ").append(registered.size()).append(" registered for >").append(name).append("<").toString());
        ArrayList<NotificationRequest> notificationCandidates = new ArrayList<NotificationRequest>();
        synchronized (registered) {
            Iterator<NotificationRequest> registeredIterator = registered.iterator();
            while (registeredIterator.hasNext()) {
                NotificationRequest interestedClient = registeredIterator.next();
                StringBuilder query = interestedClient.getQuery();
                interestedClient.setData(storageMan.executeQuery(query));
                if (logger.isInfoEnabled())
                    logger.info(new StringBuilder().append("Evaluating INTERESTED QUERY ").append(" : ").append(query).append(">>> NEEDS NOTIFICATION = ").append(interestedClient.needNotification()).toString());
                if (interestedClient.needNotification())
                    notificationCandidates.add(interestedClient);
                else if (logger.isDebugEnabled())
                			logger.debug("No notification is needed for the client");
            }
        }
        // SimulationResult.addJustQueryEvaluationFinished (registered.size()) ;
        /**
         * The following lines are commented for the simulation.
         */
        for (NotificationRequest request : notificationCandidates) {
            boolean result = request.send();
            if (result == false) {
                if (logger.isInfoEnabled())
                    logger.info("Notification Failed, removing the query.");
                removeNotificationRequest(request);
            }
        }
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        int requestType = -1;
        try {
            requestType = Integer.parseInt((String) req.getHeader(Container.REQUEST));
        } catch (Exception e) {
            logger.debug("A request received with an invalid request Type", e);
            return;
        }
        if (requestType == Container.LIST_VIRTUAL_SENSORS) {
            Iterator<VSensorConfig> vsIterator = Mappings.getAllVSensorConfigs();
            StringBuilder sb = new StringBuilder();
            while (vsIterator.hasNext()) {
                sb.append(vsIterator.next().getVirtualSensorName());
                sb.append(",");
            }
            res.setHeader(Container.RES_STATUS, Container.REQUEST_HANDLED_SUCCESSFULLY);
            res.setHeader(Container.RESPOND, sb.toString());
            return;
        }

        if (requestType == Container.DATA_PACKET) {
            String notificiationCode = req.getHeader(Container.NOTIFICATION_CODE);
            RemoteDS remoteDS = notificationCodeToRemoteDataSource.get(notificiationCode);
            if (remoteDS == null) { // This client is no more interested
                // in this notificationCode.
                res.setHeader(Container.RES_STATUS, Container.INVALID_REQUEST);
                if (logger.isInfoEnabled())
                    logger.info("Invalid notification code recieved, query droped.");
            } else {
                res.setHeader(Container.RES_STATUS, Container.REQUEST_HANDLED_SUCCESSFULLY);
                ObjectInputStream objectInputStream = new ObjectInputStream(req.getInputStream());
                Serializable deserializeFromString;
                try {
                    deserializeFromString = (Serializable) objectInputStream.readObject();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                StreamElement data = (StreamElement) deserializeFromString;
                /**
                 * If the received stream element from remote stream producer
                 * doesn't have the TIMED field inside, the<br>
                 * container will automatically insert the receive time to the
                 * stream element.<br>
                 */
                if (! data.isTimestampSet())
                    data.setTimeStamp(System.currentTimeMillis());

                StorageManager.getInstance().insertData(notificiationCode, data);
                if (logger.isInfoEnabled())
                    logger.info(new StringBuilder().append("data received for notification code *").append(notificiationCode).toString());
                remoteDS.remoteDataReceived();
            }
            return;
        }

        String prespectiveVirtualSensor = req.getHeader(Container.QUERY_VS_NAME);
        if (prespectiveVirtualSensor == null) {
            logger.warn("Bad request received for Data_strctutes");
            res.setHeader(Container.RES_STATUS, Container.INVALID_REQUEST);
            return;
        }
        VSensorConfig sensorConfig = Mappings.getVSensorConfig(prespectiveVirtualSensor);
        if (sensorConfig == null) {
            logger.warn("Requested virtual sensor doesn't exist >" + prespectiveVirtualSensor + "<.");
            res.setHeader(Container.RES_STATUS, Container.INVALID_REQUEST);
            return;
        }
        if (requestType == Container.ONE_SHOT_QUERY_EXECUTION_REQUEST) {
            String query = req.getHeader(Container.VS_QUERY);
            if (query == null) {
                res.setHeader(Container.RES_STATUS, Container.INVALID_REQUEST);
                logger.info("A request for an execution received without a query ?!!");
                return;
            }
            res.setHeader(Container.RES_STATUS, Container.REQUEST_HANDLED_SUCCESSFULLY);
            Enumeration<StreamElement> rs = StorageManager.getInstance().executeQuery(new StringBuilder(query));
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(res.getOutputStream()));
            while (rs.hasMoreElements()) {
                oos.writeObject(rs.nextElement());
                oos.flush();
            }
            oos.close();
            return;
        }
        if (requestType == Container.DATA_STRCTURE_REQUEST) {
            res.setHeader(Container.RES_STATUS, Container.REQUEST_HANDLED_SUCCESSFULLY);
            if (logger.isInfoEnabled())
                logger.info(new StringBuilder().append("Structure request for *").append(prespectiveVirtualSensor).append("* received.").toString());
            ArrayList<DataField> datafields = sensorConfig.getOutputStructure();
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(res.getOutputStream()));
            oos.writeObject(datafields);
            oos.flush();
            oos.close();
            if(logger.isDebugEnabled())
            	logger.debug("Respond sent to the requestee.");
            return;
        }
        if (requestType == Container.REGISTER_PACKET || requestType == Container.DEREGISTER_PACKET) {
            GSNNotification interest = new GSNNotification(req);
            if (requestType == Container.REGISTER_PACKET) {
                prespectiveVirtualSensor = interest.getPrespectiveVirtualSensor();
                if (logger.isInfoEnabled())
                    logger.info(new StringBuilder()
                            .append("REGISTER REQUEST FOR ").append(prespectiveVirtualSensor).append(" received from :").append(interest
                            .getRemoteAddress()).append(":").append(interest.getRemotePort()).append(" under the code ").append(interest
                            .getNotificationCode()).append(" With the query of *").append(interest.getQuery()).append("*").toString());
                addNotificationRequest(prespectiveVirtualSensor, interest);
            } else if (requestType == Container.DEREGISTER_PACKET) {
                if (logger.isInfoEnabled())
                    logger.info(new StringBuilder()
                            .append("DEREGISTER REQUEST FOR ").append(interest.getPrespectiveVirtualSensor()).append(" received").toString());
                removeNotificationRequest(interest.getPrespectiveVirtualSensor(), interest);
            }
            if(logger.isDebugEnabled())
            	logger.debug("Respond sent to the requestee.");
            return;
        }
    }

    public synchronized void addNotificationRequest(String localVirtualSensorName, NotificationRequest notificationRequest) {
        localVirtualSensorName = localVirtualSensorName.toUpperCase();
        ArrayList<NotificationRequest> contents;
        if (notificationRequests.get(localVirtualSensorName) == null) {
            contents = new ArrayList<NotificationRequest>();
            notificationRequests.put(localVirtualSensorName, contents);
        } else
            contents = notificationRequests.get(localVirtualSensorName);
        if (logger.isDebugEnabled()) {
        	logger.debug("Notification request added to "+localVirtualSensorName);
        }
        synchronized (contents) {
            contents.add(notificationRequest);
        }
    }

    public synchronized void removeNotificationRequest(String localVirtualSensorName, NotificationRequest notificationRequest) {
        localVirtualSensorName = localVirtualSensorName.toUpperCase();
        ArrayList<NotificationRequest> contents = notificationRequests.get(localVirtualSensorName);
        if (contents == null) {// when an invalid remove request recevied for
            // a
            // virtual sensor which doesn't have any query
            // registered to it.
            return;
        }
        synchronized (contents) {
            boolean changed = contents.remove(notificationRequest);
        }
    }

    public synchronized void removeNotificationRequest(NotificationRequest notificationRequest) {
        Iterator<String> virtualSensorNames = notificationRequests.keySet().iterator();
        while (virtualSensorNames.hasNext()) {
            String virtualSensorName = virtualSensorNames.next();
            ArrayList<NotificationRequest> contents = notificationRequests.get(virtualSensorName);
            if (contents == null || contents.size() == 0) {// when an invalid
                // remove request
                // recevied for a
                // virtual sensor which
                // doesn't have any
                // query registered to
                // it.
                return;
            }
            synchronized (contents) {
                boolean changed = contents.remove(notificationRequest);
            }
        }
    }

    public synchronized NotificationRequest [ ] getAllNotificationRequests() {
        Vector<NotificationRequest> results = new Vector<NotificationRequest>();
        for (ArrayList<NotificationRequest> notifications : notificationRequests.values())
            results.addAll(notifications);
        return results.toArray(new NotificationRequest [ ]{});
    }

    public void addRemoteStreamSource(String notificationCode, RemoteDS remoteDS) {
        notificationCodeToRemoteDataSource.put(notificationCode, remoteDS);
        if (logger.isDebugEnabled())
            logger.debug(new StringBuilder()
                    .append("Remote DataSource DBALIAS *").append(remoteDS.getDBAlias()).append("* with the code : *").append(notificationCode)
                    .append("* added.").toString());
    }

    public void removeAllResourcesAssociatedWithVSName(String vsensorName) {
        ArrayList<NotificationRequest> effected = notificationRequests.remove(vsensorName);
        // FIXME : The used prepare statements should be released from the
        // stroagemanager using a timeout mechanism.
        // PreparedStatement ps;
        // synchronized (psLock) {
        // ps = preparedStatements.remove(vsensorName);
        // }
        // StorageManager.getInstance().returnPrepaedStatement(ps);
    }

    public void removeRemoteStreamSource(String notificationCode) {
        notificationCodeToRemoteDataSource.remove(notificationCode);
    }

}