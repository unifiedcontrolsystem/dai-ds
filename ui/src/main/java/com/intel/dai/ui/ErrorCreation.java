package com.intel.dai.ui;

import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;


/*Will create a generalized Map with a error message inside it*/
class ErrorCreation {

    ErrorCreation (String message) {
        errorMessage = message;
        result = new PropertyMap();
    }

    private PropertyArray constructSingleMessageSchema(String message) {
        PropertyArray schema = new PropertyArray();
        PropertyMap columnInfo = new PropertyMap();
        columnInfo.put("data", message);
        columnInfo.put("unit", "string");
        columnInfo.put("heading", message);
        schema.add(columnInfo);
        return schema;
    }

    private PropertyArray constructSingleMessageData(String message) {
        PropertyArray data = new PropertyArray();
        PropertyArray result = new PropertyArray();
        result.add(message);
        data.add(result);
        return data;
    }

    public PropertyMap constructErrorResult() {
        result.put("result-data-columns" ,1);
        result.put("result-data-lines" , 1);
        result.put("result-status-code" ,1);
        result.put("schema", constructSingleMessageSchema("Message"));
        result.put("error", constructSingleMessageData(errorMessage));
        return result;
    }

    private String errorMessage;
    private PropertyMap result;
}
