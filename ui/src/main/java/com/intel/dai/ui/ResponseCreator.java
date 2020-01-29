package com.intel.dai.ui;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;

import java.util.ArrayList;
import java.util.Map;

public class ResponseCreator {

    ResponseCreator() {
        jsonParser_ = ConfigIOFactory.getInstance("json");
        assert jsonParser_ != null : "Failed to create a JSON parser!";
    }

    void setParser(ConfigIO parser) {
        jsonParser_ = parser;
    }

    public String toString(PropertyDocument document) {
        return jsonParser_.toString(document);
    }

    PropertyDocument fromString(String json) throws ConfigIOParseException {
        return jsonParser_.fromString(json);
    }

    String createJsonResult(String[] sa){
        PropertyMap result = new PropertyMap();
        result.put("Status", sa[0]);
        result.put("Result", sa[1]);
        return jsonParser_.toString(result);
    }

    String concatControlJsonResponses(ArrayList<String> results) throws ConfigIOParseException {
        PropertyMap output = new PropertyMap();
        for (String r: results)
            output.putAll((PropertyMap) jsonParser_.fromString(r));
        return jsonParser_.toString(output);
    }

    private PropertyMap convertToStandardFormat(PropertyMap input) {
        PropertyMap result = new PropertyMap();
        result.put("result-data-columns" ,2);
        result.put("result-data-lines" ,input.size());
        result.put("result-status-code" ,0);
        result.put("schema", createSchema());
        result.put("data" ,createData(input));
        return result;
    }

    private PropertyArray createData(PropertyMap input) {
        PropertyArray resultData = new PropertyArray();
        for (Map.Entry<String,Object> entry : input.entrySet()) {
            PropertyArray rowData = new PropertyArray();
            rowData.add(entry.getKey());
            rowData.add(entry.getValue());
            resultData.add(rowData);
        }
        return resultData;
    }

    private PropertyArray createSchema() {

        PropertyArray schema = new PropertyArray();
        String[] columns = new String[] {"Location", "Message"};
        for (String columnName: columns) {
            PropertyMap columnInfo = new PropertyMap();
            columnInfo.put("data", columnName);
            columnInfo.put("unit", "string");
            columnInfo.put("heading", columnName);
            schema.add(columnInfo);
        }
        return schema;
    }

    String concatProvisionerJsonResponses(ArrayList<String> results) throws ConfigIOParseException {
        PropertyMap output = new PropertyMap();
        for (String r: results)
            output.putAll((PropertyMap) jsonParser_.fromString(r));

        return jsonParser_.toString(convertToStandardFormat(output));
    }

    private ConfigIO jsonParser_ = null;
}
