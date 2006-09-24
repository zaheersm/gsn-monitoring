package gsn.beans ;

import gsn.utils.GSNRuntimeException ;

import java.sql.Types ;
import java.util.regex.Pattern ;

import org.apache.log4j.Logger ;

public class DataTypes {
   public final static String OPTIONAL_NUMBER_PARAMETER = "\\s*(\\(\\s*\\d+\\s*\\))?" ;

   public final static String REQUIRED_NUMBER_PARAMETER = "\\s*\\(\\s*\\d+\\s*\\)" ;

   private final static transient Logger logger = Logger.getLogger ( DataTypes.class ) ;

   // NEXT FIELD
   public final static String VAR_CHAR_PATTERN_STRING = "\\s*varchar" + REQUIRED_NUMBER_PARAMETER + "\\s*" ;

   public final static int VARCHAR = 0 ;

   public final static String VARCHAR_NAME = "Varchar" ;

   // NEXT FIELD
   public final static String CHAR_PATTERN_STRING = "\\s*char" + REQUIRED_NUMBER_PARAMETER + "\\s*" ;

   public final static int CHAR = 1 ;

   public final static String CHAR_NAME = "Char" ;

   // NEXT FIELD
   public final static String INTEGER_PATTERN_STRING = "\\s*((INTEGER)|(INT))\\s*" ;

   public final static int INTEGER = 2 ;

   public final static String INTEGER_NAME = "Integer" ;

   // NEXT FIELD
   public final static String BIGINT_PATTERN_STRING = "\\s*BIGINT\\s*" ;

   public final static int BIGINT = 3 ;

   public final static String BIGINT_NAME = "BigInt" ;

   // NEXT FIELD
   public final static String BINARY_PATTERN_STRING = "\\s*BINARY" + OPTIONAL_NUMBER_PARAMETER + "(\\s*:\\s*\\w*)?\\s*" ;

   public final static int BINARY = 4 ;

   public final static String BINARY_NAME = "Binary" ;

   // NEXT FIELD
   public final static String DOUBLE_PATTERN_STRING = "\\s*DOUBLE\\s*" ;

   public final static int DOUBLE = 5 ;

   public final static String DOUBLE_NAME = "Double" ;

   // NEXT FIELD
   public final static String TIME_PATTERN_STRING = "\\s*TIME\\s*" ;

   public final static int TIME = 6 ;

   public final static String TIME_NAME = "Time" ;

   // NEXT FIELD
   public final static String TINYINT_PATTERN_STRING = "\\s*TINYINT\\s*" ;

   public final static int TINYINT = 7 ;

   public final static String TINYINT_NAME = "TinyInt" ;

   // NEXT FIELD
   public final static String SMALLINT_PATTERN_STRING = "\\s*SMALLINT\\s*" ;

   public final static int SMALLINT = 8 ;

   public final static String SMALLINT_NAME = "SmallInt" ;

   // FINISH
   public final static Pattern [ ] ALL_PATTERNS = new Pattern [ ] { Pattern.compile ( VAR_CHAR_PATTERN_STRING , Pattern.CASE_INSENSITIVE ) ,
         Pattern.compile ( CHAR_PATTERN_STRING , Pattern.CASE_INSENSITIVE ) , Pattern.compile ( INTEGER_PATTERN_STRING , Pattern.CASE_INSENSITIVE ) ,
         Pattern.compile ( BIGINT_PATTERN_STRING , Pattern.CASE_INSENSITIVE ) , Pattern.compile ( BINARY_PATTERN_STRING , Pattern.CASE_INSENSITIVE ) ,
         Pattern.compile ( DOUBLE_PATTERN_STRING , Pattern.CASE_INSENSITIVE ) , Pattern.compile ( TIME_PATTERN_STRING , Pattern.CASE_INSENSITIVE ) ,
         Pattern.compile ( TINYINT_PATTERN_STRING , Pattern.CASE_INSENSITIVE ) , Pattern.compile ( SMALLINT_PATTERN_STRING , Pattern.CASE_INSENSITIVE ) } ;

   public final static StringBuilder ERROR_MESSAGE = new StringBuilder (
         "Acceptable types are (TINYINT, SMALLINT, INTEGER,BIGINT,CHAR(#),BINARY[(#)],VARCHAR(#),DOUBLE,TIME)." ) ;

   public final static String [ ] TYPE_NAMES = new String [ ] { VARCHAR_NAME , CHAR_NAME , INTEGER_NAME , BIGINT_NAME , BINARY_NAME , DOUBLE_NAME , TIME_NAME ,
         TINYINT_NAME , SMALLINT_NAME } ;

   public static int convertTypeNameToTypeID ( String type ) {
      if ( type == null )
         throw new GSNRuntimeException ( new StringBuilder ( "The type *null* is not recoginzed by GSN." ).append ( DataTypes.ERROR_MESSAGE ).toString ( ) ) ;
      for ( int i = 0 ; i < DataTypes.ALL_PATTERNS.length ; i ++ )
         if ( DataTypes.ALL_PATTERNS [ i ].matcher ( type ).matches ( ) )
            return i ;
      throw new GSNRuntimeException ( new StringBuilder ( "The type *" )
            .append ( type ).append ( "* is not recognized." ).append ( DataTypes.ERROR_MESSAGE ).toString ( ) ) ;
   }

   public static Integer convertFromJDBCToGSNFormat ( int colTypeInJDBCFormat ) {
      switch ( colTypeInJDBCFormat ) {
      case Types.BIGINT :
         return BIGINT ;
      case Types.INTEGER :
         return INTEGER ;
      case Types.SMALLINT :
         return SMALLINT ;
      case Types.TINYINT :
         return TINYINT ;
      case Types.VARCHAR :
         return VARCHAR ;
      case Types.CHAR :
         return CHAR ;
      case Types.TIME :
         return TIME ;
      case Types.DOUBLE :
         return DOUBLE ;
      case Types.BINARY :
      case Types.BLOB :
      case Types.LONGVARBINARY :
         return BINARY ;
      default :
         logger.error ( "The type can't be converted to GSN form : " + colTypeInJDBCFormat ) ;
         break ;
      }
      return null ;
   }
}