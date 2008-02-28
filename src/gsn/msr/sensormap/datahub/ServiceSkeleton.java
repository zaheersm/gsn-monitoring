/**
 * ServiceSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.3  Built on : Aug 10, 2007 (04:45:47 LKT)
 */
package gsn.msr.sensormap.datahub;

import gsn.Mappings;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.tempuri.ArrayOfArrayOfSensorData;
import org.tempuri.ArrayOfDateTime;
import org.tempuri.ArrayOfDouble;
import org.tempuri.ArrayOfSensorData;
import org.tempuri.GetAggregateScalarDataInBatchResponse;
import org.tempuri.GetAggregateScalarDataSeriesInBatchResponse;
import org.tempuri.GetLatestScalarDataInBatchResponse;
import org.tempuri.SensorData;

/**
 *  ServiceSkeleton java skeleton for the axisService
 */
public class ServiceSkeleton {
  
  private static final transient Logger         logger          = Logger.getLogger( ServiceSkeleton.class );
  /**
   * SensorTypes : 
   * public  int Unknown = 0;
   * public  int Generic = 1;
   * public  int Temperature = 2;
   * public  int Video = 3;
   * public  int Traffic = 4;
   * public  int Parking = 5;
   * public  int Pressure = 6;
   * public  int Humidity = 7;
   * **************************
   * DataTypes :
   * public  int Unknown = 0;
   * public  int Scalar = 1;
   * public  int BMP = 2;
   * public  int JPG = 3;
   * public  int GIF = 4;
   * public  int Vector = 5;
   * public  int HTML = 6;
   */
  
  /**
   * Auto generated method signature
   * @param getAggregateScalarDataSeriesInBatch
   */                  
  public org.tempuri.GetAggregateScalarDataSeriesInBatchResponse GetAggregateScalarDataSeriesInBatch(org.tempuri.GetAggregateScalarDataSeriesInBatch input) {
    GetAggregateScalarDataSeriesInBatchResponse toReturn = new GetAggregateScalarDataSeriesInBatchResponse();
    long aggInMSec = input.getAggregateIntervalInSeconds()*1000;
    ArrayOfArrayOfSensorData items = new ArrayOfArrayOfSensorData();
    for (String signalInfo: input.getSensorNames().getString()) {
      SignalRequest req = new SignalRequest(signalInfo);
      StringBuilder query = new StringBuilder("select AVG(TIMED) as TIMED,PK, AVG(").append(req.getFieldName()).append(") as data from ").append(req.getVsName()).append(" where TIMED >= ").append(input.getStartTime().getTimeInMillis()).append(" AND TIMED <= ").append(input.getEndTime().getTimeInMillis()).append(" group by TIMED/").append(aggInMSec).append(" order by TIMED");
      items.addArrayOfSensorData(transformToSensorDataArray(query));
    }
    toReturn.setGetAggregateScalarDataSeriesInBatchResult(items);
    return toReturn;
  }
  
  public org.tempuri.GetLatestScalarDataInBatchResponse GetLatestScalarDataInBatch(org.tempuri.GetLatestScalarDataInBatch input) {
    org.tempuri.GetLatestScalarDataInBatchResponse toReturn = new GetLatestScalarDataInBatchResponse();
    ArrayOfSensorData items = new ArrayOfSensorData();
    for (String signalInfo: input.getSensorNames().getString()) {
      SignalRequest req = new SignalRequest(signalInfo);
      StringBuilder query = new StringBuilder("select pk,TIMED, ").append(req.getFieldName()).append(" as data from ").append(req.getVsName()).append(" order by timed desc limit 0,1");
//    logger.fatal(query);
      items.addSensorData(transformToSensorDataArray(query).getSensorData()[0]);
    }
    toReturn.setGetLatestScalarDataInBatchResult(items);
    return toReturn;
  }
  
  public org.tempuri.GetAggregateScalarDataInBatchResponse GetAggregateScalarDataInBatch(org.tempuri.GetAggregateScalarDataInBatch input) {
    org.tempuri.GetAggregateScalarDataInBatchResponse  toReturn = new  GetAggregateScalarDataInBatchResponse();
    ArrayOfSensorData items = new ArrayOfSensorData();
    for (String signalInfo: input.getSensorNames().getString()) {
      SignalRequest req = new SignalRequest(signalInfo);
      StringBuilder query = new StringBuilder("select AVG(TIMED) as TIMED, AVG(").append(req.getFieldName()).append(") as data from ").append(req.getVsName()).append(" where TIMED >= ").append(input.getStartTime().getTimeInMillis()).append(" AND TIMED <= ").append(input.getEndTime().getTimeInMillis());
      items.addSensorData(transformToSensorDataArray(query).getSensorData()[0]);
    }
    toReturn.setGetAggregateScalarDataInBatchResult(items);
    return toReturn;
  }
  
  class SignalRequest {
    private int signal_index = -1;
    private VSensorConfig conf;
    public SignalRequest(String req) {
      StringTokenizer st= new StringTokenizer(req,"@");
      if (st.countTokens()!=2)
        throw new RuntimeException("Bad request: correct format is sensorName@FieldID , Your (invalid) request is:"+req);
      String vsName = st.nextToken();
      this.signal_index= Integer.parseInt(st.nextToken());
      logger.debug("WS-REQUEST: VSNAME : "+vsName+",VSFIELD INDEX : "+signal_index);
      this.conf =Mappings.getVSensorConfig(vsName);
      if (signal_index>=conf.getOutputStructure().length)
        throw new RuntimeException("Bad request: vs-name="+vsName+", "+signal_index+">"+conf.getOutputStructure().length);
    }
    public String getFieldName() {
      return conf.getOutputStructure()[signal_index].getName();
    }
    public String getVsName() {
      return conf.getName();
    }
  }
  
  private  ArrayOfSensorData transformToSensorDataArray(StringBuilder query) {
    System.out.println("QUERY : "+query);
    ArrayOfSensorData toReturn = new ArrayOfSensorData();
    try {
      DataEnumerator output = null;
      boolean is_binary_linked= true;
      if (query.toString().replaceAll(" ","").toLowerCase().indexOf("avg(")>0)
        is_binary_linked = false;
      output = StorageManager.getInstance().executeQuery(query, is_binary_linked);
      SensorData data = new SensorData(); 
      ArrayOfDateTime arrayOfDateTime = new ArrayOfDateTime();
      ArrayList<Double> sensor_readings = new ArrayList();
      while(output.hasMoreElements()) {
        StreamElement se = output.nextElement();
        Calendar timestamp = Calendar.getInstance();
        timestamp.setTimeInMillis(se.getTimeStamp());
        arrayOfDateTime.addDateTime(timestamp);
        sensor_readings.add(Double.parseDouble(se.getData()[0].toString()));
      }
      data.setSensorType(5);//Vector
      data.setDataType(1);// Generic
      data.setTimestamps(arrayOfDateTime);
      ArrayOfDouble arrayOfDouble = new ArrayOfDouble();
      arrayOfDouble.set_double(ArrayUtils.toPrimitive(sensor_readings.toArray(new Double[] {})));
      data.setData(arrayOfDouble);
      toReturn.addSensorData(data);
    }catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return toReturn;
  }
  
  
  
  /********************************************************************************************************/
  /********************************* AUTO GENERATED METHODS START FROM HERE *******************************/
  /********************************************************************************************************/
  
  /**
   * Auto generated method signature
   * @param registerSensor
   */
  public org.tempuri.RegisterSensorResponse RegisterSensor(
      org.tempuri.RegisterSensor registerSensor) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#RegisterSensor");
  }
  
  /**
   * Auto generated method signature
   * @param registerVectorSensor
   */
  public org.tempuri.RegisterVectorSensorResponse RegisterVectorSensor(
      org.tempuri.RegisterVectorSensor registerVectorSensor) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#RegisterVectorSensor");
  }
  
  /**
   * Auto generated method signature
   * @param deleteSensor
   */
  public org.tempuri.DeleteSensorResponse DeleteSensor(
      org.tempuri.DeleteSensor deleteSensor) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#DeleteSensor");
  }
  
  /**
   * Auto generated method signature
   * @param deleteVectorSensor
   */
  public org.tempuri.DeleteVectorSensorResponse DeleteVectorSensor(
      org.tempuri.DeleteVectorSensor deleteVectorSensor) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#DeleteVectorSensor");
  }
  
  /**
   * Auto generated method signature
   * @param updateSensorLocation
   */
  public org.tempuri.UpdateSensorLocationResponse UpdateSensorLocation(
      org.tempuri.UpdateSensorLocation updateSensorLocation) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#UpdateSensorLocation");
  }
  
  /**
   * Auto generated method signature
   * @param getSensorByPublisherAndName
   */
  public org.tempuri.GetSensorByPublisherAndNameResponse GetSensorByPublisherAndName(
      org.tempuri.GetSensorByPublisherAndName getSensorByPublisherAndName) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#GetSensorByPublisherAndName");
  }
  
  /**
   * Auto generated method signature
   * @param getSensorsByPublisher
   */
  public org.tempuri.GetSensorsByPublisherResponse GetSensorsByPublisher(
      org.tempuri.GetSensorsByPublisher getSensorsByPublisher) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#GetSensorsByPublisher");
  }
  
  /**
   * Auto generated method signature
   * @param debugSensorManager
   */
  public org.tempuri.DebugSensorManagerResponse DebugSensorManager(
      org.tempuri.DebugSensorManager debugSensorManager) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#DebugSensorManager");
  }
  
  /**
   * Auto generated method signature
   * @param debugVectorSensorManager
   */
  public org.tempuri.DebugVectorSensorManagerResponse DebugVectorSensorManager(
      org.tempuri.DebugVectorSensorManager debugVectorSensorManager) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#DebugVectorSensorManager");
  }
  
  /**
   * Auto generated method signature
   * @param storeScalarData
   */
  public org.tempuri.StoreScalarDataResponse StoreScalarData(
      org.tempuri.StoreScalarData storeScalarData) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#StoreScalarData");
  }
  
  /**
   * Auto generated method signature
   * @param storeScalarDataBatch
   */
  public org.tempuri.StoreScalarDataBatchResponse StoreScalarDataBatch(
      org.tempuri.StoreScalarDataBatch storeScalarDataBatch) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#StoreScalarDataBatch");
  }
  
  /**
   * Auto generated method signature
   * @param getLatestScalarData
   */
  public org.tempuri.GetLatestScalarDataResponse GetLatestScalarData(
      org.tempuri.GetLatestScalarData getLatestScalarData) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#GetLatestScalarData");
  }
  
  /**
   * Auto generated method signature
   * @param getScalarDataSeries
   */
  public org.tempuri.GetScalarDataSeriesResponse GetScalarDataSeries(
      org.tempuri.GetScalarDataSeries getScalarDataSeries) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#GetScalarDataSeries");
  }
  
  /**
   * Auto generated method signature
   * @param getAggregateScalarData
   */
  public org.tempuri.GetAggregateScalarDataResponse GetAggregateScalarData(
      org.tempuri.GetAggregateScalarData getAggregateScalarData) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#GetAggregateScalarData");
  }
  
  /**
   * Auto generated method signature
   * @param getAggregateScalarDataSeries
   */
  public org.tempuri.GetAggregateScalarDataSeriesResponse GetAggregateScalarDataSeries(
      org.tempuri.GetAggregateScalarDataSeries getAggregateScalarDataSeries) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#GetAggregateScalarDataSeries");
  }
  
  
  
  /**
   * Auto generated method signature
   * @param storeVectorData
   */
  public org.tempuri.StoreVectorDataResponse StoreVectorData(
      org.tempuri.StoreVectorData storeVectorData) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#StoreVectorData");
  }
  
  /**
   * Auto generated method signature
   * @param storeVectorDataByComponentIndex
   */
  public org.tempuri.StoreVectorDataByComponentIndexResponse StoreVectorDataByComponentIndex(
      org.tempuri.StoreVectorDataByComponentIndex storeVectorDataByComponentIndex) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#StoreVectorDataByComponentIndex");
  }
  
  /**
   * Auto generated method signature
   * @param getLatestVectorData
   */
  public org.tempuri.GetLatestVectorDataResponse GetLatestVectorData(
      org.tempuri.GetLatestVectorData getLatestVectorData) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#GetLatestVectorData");
  }
  
  /**
   * Auto generated method signature
   * @param getLatestVectorDataByComponentIndex
   */
  public org.tempuri.GetLatestVectorDataByComponentIndexResponse GetLatestVectorDataByComponentIndex(
      org.tempuri.GetLatestVectorDataByComponentIndex getLatestVectorDataByComponentIndex) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#GetLatestVectorDataByComponentIndex");
  }
  
  /**
   * Auto generated method signature
   * @param storeBinaryData
   */
  public org.tempuri.StoreBinaryDataResponse StoreBinaryData(
      org.tempuri.StoreBinaryData storeBinaryData) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#StoreBinaryData");
  }
  
  /**
   * Auto generated method signature
   * @param getLatestBinarySensorData
   */
  public org.tempuri.GetLatestBinarySensorDataResponse GetLatestBinarySensorData(
      org.tempuri.GetLatestBinarySensorData getLatestBinarySensorData) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#GetLatestBinarySensorData");
  }
  
  /**
   * Auto generated method signature
   * @param dataToString
   */
  public org.tempuri.DataToStringResponse DataToString(
      org.tempuri.DataToString dataToString) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#DataToString");
  }
  
  /**
   * Auto generated method signature
   * @param debugScalarDataManager
   */
  public org.tempuri.DebugScalarDataManagerResponse DebugScalarDataManager(
      org.tempuri.DebugScalarDataManager debugScalarDataManager) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#DebugScalarDataManager");
  }
  
  /**
   * Auto generated method signature
   * @param debugBinaryDataManager
   */
  public org.tempuri.DebugBinaryDataManagerResponse DebugBinaryDataManager(
      org.tempuri.DebugBinaryDataManager debugBinaryDataManager) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#DebugBinaryDataManager");
  }
  
  /**
   * Auto generated method signature
   * @param debugVectorDataManager
   */
  public org.tempuri.DebugVectorDataManagerResponse DebugVectorDataManager(
      org.tempuri.DebugVectorDataManager debugVectorDataManager) {
    //TODO : fill this with the necessary business logic
    throw new java.lang.UnsupportedOperationException("Please implement " +
        this.getClass().getName() + "#DebugVectorDataManager");
  }
}