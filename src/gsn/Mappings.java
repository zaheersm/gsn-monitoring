package gsn ;

import gsn.beans.DataField ;
import gsn.beans.VSensorConfig ;
import gsn.control.VSensorInstance ;
import gsn.utils.CaseInsensitiveComparator ;
import gsn.vsensor.Container ;
import gsn.vsensor.VirtualSensorPool ;

import java.util.Iterator ;
import java.util.TreeMap ;

import org.apache.log4j.Logger ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public final class Mappings {

   private static final TreeMap < String , VSensorConfig > vsNameTOVSConfig = new TreeMap < String , VSensorConfig > ( new CaseInsensitiveComparator ( ) ) ;

   private static final TreeMap < String , VSensorInstance > fileNameToVSInstance = new TreeMap < String , VSensorInstance > ( new CaseInsensitiveComparator ( ) ) ;

   private static final TreeMap < String , TreeMap < String , Boolean >> vsNamesToOutputStructureFields = new TreeMap < String , TreeMap < String , Boolean >> (
         new CaseInsensitiveComparator ( ) ) ;

   private static final transient Logger logger = Logger.getLogger ( Mappings.class ) ;

   private static Container container = null ;

   public static boolean addVSensorInstance ( VSensorInstance instance ) {
      VirtualSensorPool sensorPool = new VirtualSensorPool ( instance ) ;
      if ( logger.isInfoEnabled ( ) )
         logger
               .info ( ( new StringBuilder ( "Preparing the pool for: " ) )
                     .append ( instance.getConfig ( ).getVirtualSensorName ( ) ).append ( " with the max size of:" ).append ( sensorPool.getMaxSize ( ) )
                     .toString ( ) ) ;
      try {
         if ( logger.isInfoEnabled ( ) )
            logger.info ( ( new StringBuilder ( "Testing the pool for :" ) ).append ( instance.getConfig ( ).getVirtualSensorName ( ) ).toString ( ) ) ;
         sensorPool.returnInstance ( sensorPool.borrowObject ( ) ) ;
      } catch ( Exception e ) {
         logger.error ( e.getMessage ( ) , e ) ;
         sensorPool.closePool ( ) ;
         logger.error ( "GSN can't load the virtual sensor specified at " + instance.getFilename ( )
               + " because the initialization of the virtual sensor failed (see above exception)." ) ;
         logger.error ( "Please fix the following error" ) ;
         return false ;
      }
      instance.setPool ( sensorPool ) ;
      TreeMap < String , Boolean > vsNameToOutputStructureFields = new TreeMap < String , Boolean > ( new CaseInsensitiveComparator ( ) ) ;
      vsNamesToOutputStructureFields.put ( instance.getConfig ( ).getVirtualSensorName ( ) , vsNameToOutputStructureFields ) ;
      for ( DataField fields : instance.getConfig ( ).getOutputStructure ( ) )
         vsNameToOutputStructureFields.put ( fields.getFieldName ( ) , Boolean.TRUE ) ;
      vsNameToOutputStructureFields.put ( "TIMED" , Boolean.TRUE ) ;
      vsNameTOVSConfig.put ( instance.getConfig ( ).getVirtualSensorName ( ) , instance.getConfig ( ) ) ;
      fileNameToVSInstance.put ( instance.getFilename ( ) , instance ) ;
      return true ;
   }

   public static VSensorInstance getVSensorInstanceByFileName ( String fileName ) {
      return fileNameToVSInstance.get ( fileName ) ;
   }

   public static final TreeMap < String , Boolean > getVsNamesToOutputStructureFieldsMapping ( String vsName ) {
      return vsNamesToOutputStructureFields.get ( vsName ) ;
   }

   public static VSensorConfig getVSensorConfig ( String vSensorName ) {
      if ( vSensorName == null )
         return null ;
      return vsNameTOVSConfig.get ( vSensorName ) ;
   }

   public static void removeFilename ( String fileName ) {
      VSensorConfig config = ( ( VSensorInstance ) fileNameToVSInstance.get ( fileName ) ).getConfig ( ) ;
      vsNameTOVSConfig.remove ( config.getVirtualSensorName ( ) ) ;
      fileNameToVSInstance.remove ( fileName ) ;
   }

   public static Long getLastModifiedTime ( String configFileName ) {
      return Long.valueOf ( ( ( VSensorInstance ) fileNameToVSInstance.get ( configFileName ) ).getLastModified ( ) ) ;
   }

   public static String [ ] getAllKnownFileName ( ) {
      return ( String [ ] ) fileNameToVSInstance.keySet ( ).toArray ( new String [ 0 ] ) ;
   }

   public static VSensorConfig getConfigurationObject ( String fileName ) {
      if ( fileName == null )
         return null ;
      return ( ( VSensorInstance ) fileNameToVSInstance.get ( fileName ) ).getConfig ( ) ;
   }

   static void setContainer ( Container theContainer ) {
      container = theContainer ;
   }

   public static Container getContainer ( ) {
      return container ;
   }

   public static Iterator < VSensorConfig > getAllVSensorConfigs ( ) {
      return vsNameTOVSConfig.values ( ).iterator ( ) ;
   }

   public static VSensorInstance getVSensorInstanceByVSName ( String vsensorName ) {
      if ( vsensorName == null )
         return null ;
      VSensorConfig vSensorConfig = vsNameTOVSConfig.get ( vsensorName ) ;
      if ( vSensorConfig == null )
         return null ;
      return getVSensorInstanceByFileName ( vSensorConfig.getFileName ( ) ) ;
   }

}