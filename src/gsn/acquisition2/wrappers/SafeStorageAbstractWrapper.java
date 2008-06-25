package gsn.acquisition2.wrappers;

import java.net.InetSocketAddress;

import gsn.acquisition2.client.MessageHandler;
import gsn.acquisition2.client.SafeStorageClientSessionHandler;
import gsn.acquisition2.messages.DataMsg;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.wrappers.AbstractWrapper;
import org.apache.log4j.Logger;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
/**
 * Required parameters: 
 * ss-port
 * ss-host
 * wrapper-name
 *
 */
public abstract class SafeStorageAbstractWrapper extends AbstractWrapper implements MessageHandler{
  
  private final transient Logger     logger                 = Logger.getLogger ( SafeStorageAbstractWrapper.class );

  public void finalize() {
    // TODO
  }

  public String getWrapperName() {
    return "Safe Storage Proxy - "+key;
  }
  
  String key,ss_host;
  AddressBean wrapperDetails;
  int ss_port;
  
  public boolean initialize() {
    String wrapper = getActiveAddressBean().getPredicateValue("wrapper-name");
    String vs = getActiveAddressBean().getVirtualSensorName();
    String inputStreamName = getActiveAddressBean().getInputStreamName();
    wrapperDetails = getActiveAddressBean();
    key = new StringBuilder(vs).append("/").append(inputStreamName).append("/").append(wrapper).toString();
    ss_host = getActiveAddressBean().getPredicateValue("ss-host");
    ss_port = getActiveAddressBean().getPredicateValueAsInt("ss-port",-1);
    return true;
  }
  public void run() {
    connect(ss_host,ss_port,wrapperDetails,this,key); 
  }
  
 /**
  * HELPER METHOD FOR CONNECTING TO STORAGE SERVER
  */
  public void connect(String host,int port,AddressBean wrapperDetails,MessageHandler handler,String requester) {
    int CONNECT_TIMEOUT = 30; // seconds
    SocketConnector connector = new SocketConnector();
    // Change the worker timeout to 1 second to make the I/O thread quit soon
    // when there's no connection to manage.
    connector.setWorkerTimeout(1);
    // Configure the service.
    SocketConnectorConfig cfg = new SocketConnectorConfig();
    cfg.setConnectTimeout(CONNECT_TIMEOUT);
    cfg.getFilterChain().addLast("codec",   new ProtocolCodecFilter( new ObjectSerializationCodecFactory()));
    IoSession session = null;
    try {
      ConnectFuture future = connector.connect(new InetSocketAddress(host, port), new SafeStorageClientSessionHandler(wrapperDetails,handler,key ), cfg);
      future.join();
      session = future.getSession();
    } catch (RuntimeIOException e) {
      logger.error("Failed to connect to "+host+":"+port); 
      logger.error( e.getMessage(),e);
    }finally {
      if (session!=null)
        session.getCloseFuture().join();
    }
  }
}