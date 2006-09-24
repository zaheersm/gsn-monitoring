package gsn.wrappers ;

import gsn.Main;
import gsn.beans.InputStream;
import gsn.beans.StreamSource;
import gsn.control.VSensorLoader;
import gsn.storage.SQLUtils;
import gsn.storage.StorageManager;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 *         Date: Aug 4, 2005 <br>
 *         Time: 10:15:42 PM <br>
 */
public class DataListener {

    private final transient Logger logger = Logger.getLogger(DataListener.class);

    protected StreamSource streamSource;

    /**
     * The name is the very name of the Table name in the storage manager which
     * is used for storing data; (e.g., DataBaseAliasName).
     */

    protected InputStream inputStream;

    protected StorageManager storageManager;

    protected String viewName;

    protected transient StringBuilder viewQery;

    private transient String cachedMergedQuery = null;

    private transient StringBuffer cachedWhereClause = null;

    public void initialize(TreeMap map) {
        this.streamSource = (StreamSource) map.get(VSensorLoader.STREAM_SOURCE);
        this.inputStream = (InputStream) map.get(VSensorLoader.INPUT_STREAM);
        this.inputStream.addToRenamingMapping(streamSource.getAlias(), getViewName());
        this.storageManager = (StorageManager) map.get(VSensorLoader.STORAGE_MANAGER);
    }

    public boolean isCountBased() {
        return streamSource.isStorageCountBased();
    }

    public int getHistorySize() {
        return streamSource.getParsedStorageSize();
    }

    public StreamSource getStreamSource() {
        return streamSource;
    }

    public void dataAvailable() {
        if (logger.isDebugEnabled())
            logger.debug(new StringBuilder().append("Data avialble in the stream *").append(streamSource.getAlias()).append("*").toString());
        inputStream.dataAvailable(streamSource.getAlias());
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getViewName() {
        if (viewName == null)
            viewName = Main.tableNameGenerator();
        return viewName;
    }

    public StringBuilder getViewQuery() {
        if (viewQery == null)
            viewQery = new StringBuilder("select * from " + getViewName().replace("\"", ""));
        return viewQery;
    }

    public boolean equals(Object obj) {
        if (obj == null || ! (obj instanceof DataListener))
            return false;
        DataListener ds = (DataListener) obj;
        return (ds.getViewName().equals(getViewName()));
    }

    public int hashCode() {
        return getViewName().hashCode();
    }

    public void finalize(HashMap map) {
    }

    public String getMergedQuery() {
        if (cachedMergedQuery == null)
            cachedMergedQuery = generateMergedSqlQuery(this);
        return cachedMergedQuery;
    }

    protected String generateMergedSqlQuery(DataListener dataListener) {
        StreamSource streamSrc = dataListener.getStreamSource();
        StringBuilder toReturn = new StringBuilder(streamSrc.getSqlQuery());
        boolean needsWhere = (toReturn.toString().toUpperCase().indexOf(" WHERE ") <= 0);

        if (needsWhere) // Applying the ** End Time **
            toReturn.append(" where ").append(" (\"wrapper.TIMED\" <=").append(streamSrc.getEndDate().getTime()).append(")");
        else {
            toReturn.append(" AND (\"wrapper.TIMED\" <=").append(streamSrc.getEndDate().getTime()).append(")");
        }
        // Applying the ** START TIME **
        toReturn.append(" AND (\"wrapper.TIMED\" >=").append(streamSrc.getStartDate().getTime()).append(")");
        // Applying the ** Sampling Rate **

        float rate = streamSrc.getSamplingRate() * 100;
        if (rate == 0)
            logger.warn(new StringBuilder().append("The sampling rate is set to zero which means no results. (InputStream = ").append(dataListener.getInputStream()).append(", StreamSource = ").append(streamSrc.getAlias()).toString());
        toReturn
                .append(" AND ( mod( abs(").append(System.currentTimeMillis()).append(" - \"wrapper.TIMED\")*11  , 100)< ").append(rate)
                .append(")");

        // Applying the ** History Size **
        String historySize = streamSrc.getStorageSize();
        if (historySize != null)
            if (isInt(historySize)) { // Applying the ** Count based
                // History Size **
                int value = Integer.parseInt(historySize);
                toReturn.append(" order by \"wrapper.TIMED\" desc  limit  ").append(value).append(" offset 0");
            } else { // Applying the ** Timing based History Size **
                if (StorageManager.isHsql()) {
                    if (historySize.toLowerCase().trim().endsWith("m")) {
                        int miniute = Integer.parseInt(historySize.toLowerCase().replace("m", ""));
                        toReturn.append(" AND ((NOW_MILLIS() - \"wrapper.TIMED\") <=").append(1000 * 60 * miniute).append(" )");
                    } else if (historySize.toLowerCase().trim().endsWith("s")) {
                        int seconds = Integer.parseInt(historySize.toLowerCase().trim().replace("s", ""));
                        toReturn.append(" AND ((NOW_MILLIS() - \"wrapper.TIMED\") <=").append(1000 * seconds).append(" )");
                    } else if (historySize.toLowerCase().trim().endsWith("h")) {
                        int hours = Integer.parseInt(historySize.toLowerCase().trim().replace("h", ""));
                        toReturn.append(" AND ((NOW_MILLIS() - \"wrapper.TIMED\") <=").append(1000 * 60 * 60 * hours).append(" )");
                    }
                } else if (StorageManager.isMysqlDB()) {
                    if (historySize.toLowerCase().trim().endsWith("m")) {
                        int miniute = Integer.parseInt(historySize.toLowerCase().replace("m", ""));
                        toReturn.append(" AND ((UNIX_TIMESTAMP() - \"wrapper.TIMED\") <=").append(1000 * 60 * miniute).append(" )");
                    } else if (historySize.toLowerCase().trim().endsWith("s")) {
                        int seconds = Integer.parseInt(historySize.toLowerCase().trim().replace("s", ""));
                        toReturn.append(" AND ((UNIX_TIMESTAMP() - \"wrapper.TIMED\") <=").append(1000 * seconds).append(" )");
                    } else if (historySize.toLowerCase().trim().endsWith("h")) {
                        int hours = Integer.parseInt(historySize.toLowerCase().trim().replace("h", ""));
                        toReturn.append(" AND ((UNIX_TIMESTAMP() - \"wrapper.TIMED\") <=").append(1000 * 60 * 60 * hours).append(" )");
                    }
                }

            }
        if (logger.isDebugEnabled()) {
            logger.debug(new StringBuilder().append("The original Query : \"").append(streamSrc.getSqlQuery()).append("\"").toString());
            logger.debug(new StringBuilder()
                    .append("The merged query : ").append(toReturn.toString()).append(" of the StreamSource \"").append(streamSrc.getAlias())
                    .append("\" of the InputStream:\"").append(dataListener.getInputStream().getInputStreamName()).append("\"").toString());
        }
        return toReturn.toString();
    }

    private boolean isInt(String storageSize) {
        try {
            Integer.parseInt(storageSize.trim());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * This method returns the where clause (without where keyword) of the
     * mergedquery's result. <br>
     * This method also caches it's result in the <code>cachedWhereClause</code>.
     */
    public StringBuffer getCompleteMergedWhereClause(String remoteVSName) {
        if (cachedWhereClause == null) {
            String mergedQuery = getMergedQuery();
            int indexOrOrderBy = mergedQuery.toUpperCase().indexOf(" ORDER ");
            int indexOfWhereClause = mergedQuery.toUpperCase().indexOf(" WHERE ");
            cachedWhereClause = new StringBuffer(mergedQuery.substring(indexOfWhereClause + " WHERE ".length(), (indexOrOrderBy > 0 ? indexOrOrderBy
                    : getMergedQuery().length())));
            HashMap<String, String> rewritingMapping = new HashMap<String, String>();
            rewritingMapping.put("WRAPPER", remoteVSName);
            cachedWhereClause = new StringBuffer(SQLUtils.rewriteQuery(cachedWhereClause.toString(), rewritingMapping));
            if (logger.isDebugEnabled())
                logger.debug(new StringBuilder()
                        .append("The Complete Mereged Query's where part, rewritten for *").append(remoteVSName).append("* is ").append(cachedWhereClause
                        .toString()).toString());
        }
        return cachedWhereClause;
    }
}