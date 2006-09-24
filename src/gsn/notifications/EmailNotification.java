package gsn.notifications ;

import gsn.Main ;

import org.apache.commons.mail.SimpleEmail ;
import org.apache.commons.validator.GenericValidator ;
import org.apache.log4j.Logger ;

/**
 * IMPORTANT : FOR USING THE SMSNOTIFICATION OR EMAIL-NOTIFICATION CLASS YOU
 * NEED TO HAVE THE 1. JavaBeans Activation Framework
 * (http://java.sun.com/products/javabeans/jaf/index.jsp) and put the
 * >activation.jar< file in the lib. 2. JavaMail API :
 * (http://java.sun.com/products/javamail/downloads/index.html) and put the all
 * the jar files of JavaMail in the lib directory.
 * 
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 *         Create : May 26, 2005 <br>
 *         Created for : GSN project. <br>
 */
public class EmailNotification extends NotificationRequest {

   private static transient Logger logger = Logger.getLogger ( EmailNotification.class ) ;

   private String receiverEmailAddress ;

   /**
    * Creates a notification for email. The input is address of the system
    * receiving the email. <p/> The syntax is <code>blabla@foo.com</code>
    */

   private transient String notificationCode ;

   private String subject = "GSN-Notification" ;

   private StringBuilder query ;

   private String message ;

   private static final String fromEmail = Main.getContainerConfig ( ).getEmail ( ) ;

   private static final String mailServer = Main.getContainerConfig ( ).getMailServer ( ) ;

   private int emailCounter ;

   public EmailNotification ( String emailAddress , String query , String message ) {
      this.receiverEmailAddress = emailAddress ;
      this.query = new StringBuilder ( query.trim ( ) ) ;
      this.message = message ;
   }

   public StringBuilder getQuery ( ) {
      return query ;
   }

   public boolean send ( ) {
	  try {
         if ( ! GenericValidator.isEmail ( fromEmail ) ) {
            logger.warn ( "There is a email notification request, but the email address in container's configuration is not a valid email address" ) ;
            return false ;
         }
         SimpleEmail email = new SimpleEmail ( ) ;
         email.setHostName ( mailServer ) ;
         email.setFrom ( fromEmail ) ;
         email.addTo ( receiverEmailAddress ) ;

         email.setSubject ( subject ) ;
         email.setContent ( message , "text/plain" ) ;
         if (logger.isDebugEnabled())
  		   logger.debug("Wants to send email to "+email.getFromAddress());
  	   
         email.send ( ) ;
         
      } catch ( Exception e ) {
         if ( logger.isInfoEnabled ( ) )
            logger.info ( "Email Notification process failed, trying to notify *" + receiverEmailAddress + "*" , e ) ;
         return false ;
      }
      if ( ++ emailCounter % 3 == 0 )
         return false ;
      return true ;
   }

   public String getNotificationCode ( ) {
      if ( notificationCode == null )
         this.notificationCode = Main.tableNameGenerator ( ) ;
      return notificationCode ;
   }

   public String toString ( ) {
      StringBuffer output = new StringBuffer ( ) ;
      output
            .append ( "EmailNotification : [" ).append ( "Address = " ).append ( receiverEmailAddress ).append ( ",Query = " ).append ( query )
            .append ( ",Message = " ).append ( message ).append ( " ]" ) ;
      return output.toString ( ) ;
   }

   public String getMessage ( ) {
      return message ;
   }

   public String getReceiverEmailAddress ( ) {
      return receiverEmailAddress ;
   }

   public String getSubject ( ) {
      return subject ;
   }
}