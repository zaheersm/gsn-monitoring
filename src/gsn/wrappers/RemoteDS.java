package gsn.wrappers ;

import gsn.Main ;
import gsn.Mappings ;
import gsn.beans.DataField ;
import gsn.shared.Registry ;
import gsn.utils.TCPConnPool;
import gsn.vsensor.Container ;

import java.sql.SQLException ;
import java.util.ArrayList ;
import java.util.Collection ;
import java.util.Iterator ;
import java.util.TreeMap ;
import java.io.ObjectInputStream;
import java.io.IOException;

import org.apache.commons.httpclient.Header ;
import org.apache.commons.httpclient.methods.PostMethod ;
import org.apache.log4j.Logger ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class RemoteDS extends AbstractStreamProducer {

    private final transient Logger logger = Logger.getLogger(RemoteDS.class);

    private ArrayList<DataField> strcture = new ArrayList<DataField>();

    private String remoteVSName;

    private ArrayList<StringBuffer> registeredWhereClauses = new ArrayList<StringBuffer>();

    public boolean initialize(TreeMap initialContext) {
        boolean result = super.initialize(initialContext);
        this.remoteVSName = (String) initialContext.get(Registry.VS_NAME);
        if (this.remoteVSName == null) {
            logger.warn("The \"NAME\" paramter of the AddressBean which corresponds to the remote Virtual Sensor is missing");
            return false;
        }
        this.strcture = askForStrcture();
        if (this.strcture == null) {
            logger.warn("The initialization of the ** virtual sensor failed due to *askForStrcture* failure.");
            return false;
        }
        try {
            getStorageManager().createTable(getDBAlias(), strcture, false, false);
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        Mappings.getContainer().addRemoteStreamSource(getDBAlias(), this);
        return result;
    }

    private ArrayList<DataField> askForStrcture() {
        String host = getAddressBeanActiveHostName();
        if (host.indexOf("http://") < 0)
            host = "http://" + host;
        String destination = new StringBuilder().append(host).append(":").append(getAddressBeanActivePort()).append("/gsn").toString();
        if (logger.isInfoEnabled())
            logger.info(new StringBuilder().append("Wants to ask for structure from : ").append(destination).toString());
        PostMethod postMethod = new PostMethod(destination);
        postMethod.setRequestHeader(Container.REQUEST, Integer.toString(Container.DATA_STRCTURE_REQUEST));
        postMethod.setRequestHeader(Container.QUERY_VS_NAME, remoteVSName);
        if (logger.isDebugEnabled()) {
            logger.debug(new StringBuilder("Post request contains : ")
                    .append("QUERY_VS_NAME = ").append(remoteVSName).append(";").append("Request Type : ").append(Container.DATA_STRCTURE_REQUEST)
                    .toString());
        }
        int statusCode = TCPConnPool.executeMethod(postMethod);
        if (statusCode == - 1) {
            logger.warn(new StringBuilder()
                    .append("Message couldn't be sent to :").append(postMethod.getHostConfiguration().getHostURL()).toString());
            return null;
        }

        if (postMethod.getResponseHeader(Container.RES_STATUS) == null
                || postMethod.getResponseHeader(Container.RES_STATUS).equals(Container.REQUEST_HANDLED_SUCCESSFULLY)) {
            logger.debug("The respond from server : " + postMethod.getResponseHeader(Container.RES_STATUS));
            return null;
        }
//      ArrayList < DataField > outputStreamStruecture = new ArrayList < DataField > ( ) ;
//      Header [ ] fieldName = postMethod.getResponseHeaders ( Container.RES_HEADER_DATA_FIELD_NAME ) ;
//      Header [ ] fieldType = postMethod.getResponseHeaders ( Container.RES_HEADER_DATA_FIELD_TYPE ) ;
//      System.out.println("fieldType.length = " + fieldType.length);
//
//       for ( int i = 0 ; i < fieldType.length ; i ++ ) {
//         outputStreamStruecture.add ( new DataField ( fieldName [ i ].getValue ( ) , fieldType [ i ].getValue ( ) ) ) ;
//         if ( logger.isDebugEnabled ( ) )
//            logger.debug ( new StringBuilder ( )
//                  .append ( "Remote Data Field :" ).append ( fieldName [ i ].getValue ( ) ).append ( " = " ).append ( fieldType [ i ].getValue ( ) )
//                  .toString ( ) ) ;
//      }
        ArrayList<DataField> outputStreamStruecture = null;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(postMethod.getResponseBodyAsStream());
            outputStreamStruecture = (ArrayList<DataField>) ois.readObject();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.warn(e.getMessage(), e);
        } finally {
            if (ois != null)
                try {
                    ois.close();
                } catch (IOException e) {
                    logger.debug(e.getMessage(), e);
                }
        }

        return outputStreamStruecture;
    }

    /**
     * Sends in fact two requests, one <code>Deregister</code> request for
     * removing the previously resgistered <br>
     * query and afterwards it will send a <code>register</code> request for
     * adding the new version of the query.<br>
     */
    private void refreshRemotelyRegisteredQuery() {
        String notificationCode = getDBAlias();
        String host = getAddressBeanActiveHostName();
        String query = new StringBuffer("SELECT * FROM ")
                .append(remoteVSName).append(" WHERE ").append(getWhereClausesAllTogher()).append(" ORDER BY ").append(remoteVSName)
                .append(".TIMED DESC LIMIT 1 OFFSET 0").toString().replace("\"", "");
        if (host.indexOf("http://") < 0)
            host = "http://" + host;
        String destination = host + ":" + getAddressBeanActivePort() + "/gsn";
        if (logger.isDebugEnabled())
            logger.debug(new StringBuilder()
                    .append("Wants to send message to : ").append(destination).append("  for DEREGISTERING the previous query").toString());
        PostMethod postMethod = new PostMethod(destination);
        postMethod.addRequestHeader(Container.REQUEST, Integer.toString(Container.DEREGISTER_PACKET));
        postMethod.addRequestHeader(Registry.VS_PORT, Integer.toString(Main.getContainerConfig().getContainerPort()));
        postMethod.addRequestHeader(Container.VS_QUERY, query);
        postMethod.addRequestHeader(Container.QUERY_VS_NAME, remoteVSName);
        postMethod.addRequestHeader(Container.NOTIFICATION_CODE, notificationCode);

        int statusCode = TCPConnPool.executeMethod(postMethod);
        if (statusCode == - 1) {
            logger.warn("Message couldn't be sent to :" + postMethod.getHostConfiguration().getHostURL());
        }

        if (logger.isDebugEnabled())
            logger.debug(new StringBuilder()
                    .append("Wants to send message to : ").append(destination).append(" with the query ->").append(query).append("<-").toString());
        postMethod.setRequestHeader(Container.REQUEST, Integer.toString(Container.REGISTER_PACKET));

        statusCode = TCPConnPool.executeMethod(postMethod);
        if (statusCode == - 1) {
            logger.warn("Message couldn't be sent to :" + postMethod.getHostConfiguration().getHostURL());
        }
    }

    public String addListener(DataListener dataListener) {
        StringBuffer completeMergedWhereClause = dataListener.getCompleteMergedWhereClause(remoteVSName);
        registeredWhereClauses.add(completeMergedWhereClause);
        refreshRemotelyRegisteredQuery();
        return super.addListener(dataListener);
    }

    public void removeListener(DataListener dataListener) {
        registeredWhereClauses.remove(dataListener.getCompleteMergedWhereClause(remoteVSName));
        if (registeredWhereClauses.size() > 0)
            refreshRemotelyRegisteredQuery();
        super.removeListener(dataListener);
    }

    /**
     * The container is going to notify the <code>RemoteDS</code> about arrival
     * of a new data by calling this method. (note that, container will first
     * insert the data into the appropriate database and then calls the following
     * method).
     */
    public void remoteDataReceived() {
        if (logger.isDebugEnabled())
            logger.debug("There are results, is there any listeners ?");
        for (Iterator<DataListener> iterator = listeners.iterator(); iterator.hasNext();) {
            DataListener dataListener = iterator.next();
            boolean results = getStorageManager().isThereAnyResult(dataListener.getViewQuery());
            if (results) {
                if (logger.isDebugEnabled())
                    logger.debug(new StringBuilder().append("There are listeners, notify the ").append(dataListener
                            .getInputStream().getInputStreamName()).append(" inputStream").toString());
                // TODO :DECIDE WHETHER TO INFORM THE CLIENT OR NOT (TIME
                // TRIGGERED. DATA TRIGGERED)
                dataListener.dataAvailable();
            }
        }
    }

    public Collection<DataField> getProducedStreamStructure() {
        return strcture;
    }

    private String getWhereClausesAllTogher() {
        StringBuffer result = new StringBuffer();
        for (StringBuffer whereClause : registeredWhereClauses) {
            result.append("(").append(whereClause).append(")").append(" OR ");
        }
        return result.delete(result.length() - " OR ".length(), result.length()).toString();
    }
}