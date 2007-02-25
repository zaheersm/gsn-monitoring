package gsn.wrappers;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import java.io.File;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;

/**
 *
 * @author mehdi
 */
public class DiskSpaceWrapper extends AbstractWrapper{
    
    private static final int            DEFAULT_SAMPLING_RATE       = 1000;
    
    private int                         samplingRate                = DEFAULT_SAMPLING_RATE;
    
    private final transient Logger      logger                      = Logger.getLogger(DiskSpaceWrapper.class);
    
    private static int                  threadCounter               = 0;
    
    private transient DataField[]       outputStructureCache        = new DataField[]{new DataField("FREE_SPACE", "bigint", "Free Disk Space")};

    private File[] roots;
    
      
    public boolean initialize() {
        logger.info("Initializing DiskSpaceWrapper Class");
        if(!ManagementFactory.getRuntimeMXBean().getVmVersion().startsWith("1.6"))
            logger.error("Error in initializing DiskSpaceWrapper because of incompatible jdk version (should be 1.6.x)");
        setName("DiskSpaceWrapper-Thread" + (++threadCounter));
        return true;
    }
    
    public void run(){
        while(isActive()){
            try{
                Thread.sleep(samplingRate);
            }catch (InterruptedException e){
                logger.error(e.getMessage(), e);
            }
            roots = File.listRoots();
            long totalFreeSpace = 0;
            for (int i = 0; i < roots.length; i++) {
                totalFreeSpace += roots[i].getFreeSpace();
            }
            
            //convert to MB
            totalFreeSpace = totalFreeSpace / (1024 * 1024);
            StreamElement streamElement = new StreamElement(new String[]{"FREE_SPACE"}, new Byte[]{DataTypes.BIGINT}, new Serializable[] {totalFreeSpace
            },System.currentTimeMillis());
            postStreamElement(streamElement);
        }
    }
    
    public void finalize() {
        threadCounter--;
    }
    
    public String getWrapperName() {
        return "Free Disk Space";
    }
    
    public DataField[] getOutputFormat() {
        return outputStructureCache;
    }
    
}