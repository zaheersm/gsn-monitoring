package gsn.wrappers.general ;

import gsn.beans.AddressBean ;
import gsn.beans.DataField ;
import gsn.beans.DataTypes ;
import gsn.beans.StreamElement ;
import gsn.storage.StorageManager ;
import gsn.vsensor.Container ;
import gsn.wrappers.AbstractStreamProducer ;

import java.io.IOException ;
import java.io.InputStream ;
import java.io.Serializable ;
import java.net.DatagramPacket ;
import java.net.DatagramSocket ;
import java.net.SocketException ;
import java.sql.SQLException ;
import java.util.ArrayList ;
import java.util.Collection ;
import java.util.HashMap ;
import java.util.TreeMap ;

import org.apache.log4j.Logger ;

/**
 * Links GSN to a Wisenet sensors network. The computer running this wrapper
 * should be connected to an IP network. One of the WSN nodes should forward
 * received packets through UDP to the host running this wrapper.
 * 
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * @author Jerome Rousselot (jerome.rousselot@csem.ch), CSEM<br>
 */
public class UDPWrapper extends AbstractStreamProducer {

   private static final String RAW_PACKET = "RAW_PACKET" ;

   private final transient Logger logger = Logger.getLogger ( UDPWrapper.class ) ;

   private int threadCounter = 0 ;

   public InputStream is ;

   private AddressBean addressBean ;

   private int port ;

   private DatagramSocket socket ;

   /*
    * Needs the following information from XML file :
    * 
    * port : the udp port it should be listening to rate : time to sleep between
    * each packet
    */
   public boolean initialize ( TreeMap context ) {
      boolean status = true ;
      super.initialize ( context ) ;
      setName ( "UDPWrapper-Thread" + ( ++ threadCounter ) ) ;

      try {
         StorageManager.getInstance ( ).createTable ( getDBAlias ( ) , getProducedStreamStructure ( ) ) ;
      } catch ( SQLException e ) {
         logger.error ( e.getMessage ( ) , e ) ;
         return false ;
      }
      addressBean = ( AddressBean ) context.get ( Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN ) ;
      port = Integer.parseInt ( addressBean.getPredicateValue ( "port" ) ) ;
      // rate = Integer.parseInt(addressBean.getPredicateValue ( "rate" ));
      try {
         socket = new DatagramSocket ( port ) ;
      } catch ( SocketException e ) {
         logger.warn ( "Couldn't open UDP socket : " + e.getMessage ( ) ) ;
      }
      this.start ( ) ;
      return status ;
   }

   public void run ( ) {
      byte [ ] receivedData = new byte [ 50 ] ;
      DatagramPacket receivedPacket = null ;
      while ( isActive ( ) ) {
         try {
            receivedPacket = new DatagramPacket ( receivedData , receivedData.length ) ;
            socket.receive ( receivedPacket ) ;
            String dataRead = new String ( receivedPacket.getData ( ) ) ;
            if ( logger.isDebugEnabled ( ) )
               logger.debug ( "UDPWrapper received a packet : " + dataRead ) ;

            StreamElement streamElement = new StreamElement (
                  new String [ ] { RAW_PACKET } , new Integer [ ] { DataTypes.BINARY } , new Serializable [ ] { receivedPacket.getData ( ) } , System
                        .currentTimeMillis ( ) ) ;
            publishData ( streamElement ) ;
         } catch ( IOException e ) {
            logger.warn ( "Error while receiving data on UDP socket : " + e.getMessage ( ) ) ;
         }
      }
   }

   public Collection < DataField > getProducedStreamStructure ( ) {
      ArrayList < DataField > dataField = new ArrayList < DataField > ( ) ;
      dataField.add ( new DataField ( RAW_PACKET , "BINARY" , "The packet contains raw data received as a UDP packet." ) ) ;
      return dataField ;
   }

   public void finalize ( HashMap context ) {
      super.finalize ( context ) ;
      threadCounter -- ;
   }

   public static void main ( String [ ] args ) {
      // To check if the wrapper works properly.
      // this method is not going to be used by the system.
   }
}