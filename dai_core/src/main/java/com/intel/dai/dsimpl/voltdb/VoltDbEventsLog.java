package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.EventsLog;
import com.intel.dai.dsapi.RasEventLog;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;

import org.voltdb.VoltTable;
import org.voltdb.VoltTableRow;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VoltDbEventsLog implements EventsLog {

    public VoltDbEventsLog (String[] servers, String adapterName, String adapterType, Logger logger) {
        servers_ = servers;
        adapterName_ = adapterName;
        adapterType_ = adapterType;
        mRasDescNameToEventTypeMap = new HashMap<>();
        mEventTypeToRasDescName = new HashMap<>();
        this.logger = logger;
    }

    public void initialize() {
        initializeVoltClient(servers_);
        loadRasEventLog(servers_, adapterName_, adapterType_);
        loadRasEvtTypeDescName();
    }

    void loadRasEventLog(String[] servers, String adapterName, String adapterType) {
        _raseventlog = new VoltDbRasEventLog(servers, adapterName, adapterType, logger);
    }

    void loadRasEvtTypeDescName() {
        try {
            response = voltClient.callProcedure("@AdHoc", "select EventType, DescriptiveName from RasMetaData;");
        } catch (IOException | ProcCallException ex) {
            logger.exception(ex, "Unable to retrieve RAS meta data from the data store");
            throw new RuntimeException("Unable to retrieve RAS meta data from the data store", ex);
        }

        VoltTable rasMetaData = response.getResults()[0];
        if (response.getStatus() != ClientResponse.SUCCESS) {
            logger.error("Unable to retrieve RAS meta data from the data store. Client response status: " +
                    response.getStatus());
            throw new RuntimeException("Unable to retrieve RAS meta data from the data store. Client response status: " +
                    response.getStatus());
        }

        Map<String, String> tempDescToEventType = new HashMap<>();
        Map<String, String> tempEventTypeToDesc = new HashMap<>();
        while (rasMetaData.advanceRow()) {
            tempEventTypeToDesc.put(rasMetaData.getString("EventType"), rasMetaData.getString("DescriptiveName"));
            tempDescToEventType.put(rasMetaData.getString("DescriptiveName"), rasMetaData.getString("EventType"));
        }
        mEventTypeToRasDescName = tempEventTypeToDesc;
        mRasDescNameToEventTypeMap = tempDescToEventType;
    }

    void initializeVoltClient(String[] servers) {
        VoltDbClient.initializeVoltDbClient(servers);
        voltClient = VoltDbClient.getVoltClientInstance();
    }

    @Override
    public void createRasEvent(Map<String, String> param) {
        String eventtype = param.getOrDefault("eventtype", null);
        String location = param.getOrDefault("location", null);
        String instancedata = param.getOrDefault("instancedata", null);
        String jobid = param.getOrDefault("jobid", null);
        String checkJobBool = param.getOrDefault("checkForAffectedJob", "false");
        boolean checkJob = Boolean.parseBoolean(checkJobBool);

        if (location == null || location.isEmpty())
            checkJob = false;

        if (jobid != null && !jobid.isEmpty()) {
            _raseventlog.logRasEventWithEffectedJob(eventtype,instancedata,location,jobid,System.currentTimeMillis() * 1000L , "UI", -1L);
        }
        else  if( jobid == null && checkJob) {
            _raseventlog.logRasEventCheckForEffectedJob(eventtype,instancedata,location,System.currentTimeMillis() * 1000L, "UI", -1L);
        }
        else {
            _raseventlog.logRasEventNoEffectedJob(eventtype,instancedata,location, System.currentTimeMillis() * 1000L, "UI", -1L);
        }
    }

    @Override
    public PropertyArray listAllRasEventTypes(Map<String, String> parameters) {
        try {
            String limit = parameters.get("limit");
            String eventTypeOrDescriptive = parameters.getOrDefault("eventtype", "%");
            response = voltClient.callProcedure(GET_RASEVENT_TYPES, eventTypeOrDescriptive, eventTypeOrDescriptive, limit);
        } catch (IOException | ProcCallException ex) {
            logger.exception(ex, "Unable to retrieve RAS meta data from the data store");
            throw new RuntimeException("Unable to retrieve RAS meta data from the data store", ex);
        }

        VoltTable rasMetaData = response.getResults()[0];
        if (response.getStatus() != ClientResponse.SUCCESS) {
            logger.error("Unable to retrieve RAS meta data from the data store. Client response status: " +
                    response.getStatus());
            throw new RuntimeException("Unable to retrieve RAS meta data from the data store. Client response status: " +
                    response.getStatus());
        }
        return convertToJSON(rasMetaData);
    }

    @Override
    public PropertyArray listAllRasEventTypes() {
        try {
            response = voltClient.callProcedure(GET_RASEVENT_TYPES);
        } catch (IOException | ProcCallException ie) {
            ie.printStackTrace();
            logger.error("Error occurred while retrieving the data. ");
            throw new RuntimeException("Error occurred while retrieving the data.");
        }
        VoltTable vt = response.getResults()[0];
        return convertToJSON(vt);
    }

    @Override
    public boolean checkDescriptiveName(String descriptiveName) {
        String RasDescName = mRasDescNameToEventTypeMap.get(descriptiveName);
        if (RasDescName != null)
            return true;
        return false;
    }

    @Override
    public boolean checkRasEventType(String eventType) {
        String RasEventType = mEventTypeToRasDescName.get(eventType);
        if (RasEventType != null) {
            return true;
        }
        return  false;
    }

    private static PropertyArray convertToJSON(VoltTable resultData) {
        PropertyArray jsonArray = new PropertyArray();
        int totalRows = resultData.getRowCount();
        int totalColumns = resultData.getColumnCount();
        for (int row = 0; row < totalRows; row++) {
            PropertyMap obj = new PropertyMap();
            VoltTableRow rowData = resultData.fetchRow(row);
            for (int column = 0; column < totalColumns; column++) {
                String column_name = resultData.getColumnName(column);
                if(resultData.getColumnType(column) == VoltType.TIMESTAMP){
                    if (rowData.getTimestampAsSqlTimestamp(column_name) == null)
                        obj.put(resultData.getColumnName(column).toLowerCase(), null);
                    else
                        obj.put(column_name.toLowerCase(), rowData.getTimestampAsSqlTimestamp(column).toString());
                }
                else {
                    obj.put(resultData.getColumnName(column).toLowerCase(), rowData.get(column,
                            rowData.getColumnType(column)));
                }
            }
            jsonArray.add(obj);
        }
        return jsonArray;
    }

    private String[] servers_;
    private String adapterName_;
    private String adapterType_;
    RasEventLog _raseventlog;
    private ClientResponse response;
    Client voltClient;
    private Logger logger;
    Map<String, String> mEventTypeToRasDescName;
    Map<String, String> mRasDescNameToEventTypeMap;
    private static String GET_RASEVENT_TYPES = "getAllEventMetaData";
}
