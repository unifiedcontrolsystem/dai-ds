package com.intel.dai.ui;

import com.intel.dai.dsapi.BootImage;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.exceptions.ProviderException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;

import java.util.*;

class BootImageApi {

    BootImageApi(BootImage bootImage) {
        _bootImage = bootImage;
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

    private PropertyArray constructSingleMessageData(List<String> messages) {
        PropertyArray data = new PropertyArray();
        for( String message: messages) {
            PropertyArray result = new PropertyArray();
            result.add(message);
            data.add(result);
        }
        return data;
    }

    private PropertyMap constructGenricResult(String message) {
        PropertyMap result = new PropertyMap();
        result.put("result-data-columns" ,1);
        result.put("result-data-lines" , 1);
        result.put("result-status-code" ,0);
        result.put("schema", constructSingleMessageSchema("Message"));
        List<String> messages = new ArrayList<>();
        messages.add(message);
        result.put("data", constructSingleMessageData(messages));
        return result;
    }

    PropertyMap addBootImageProfile(Map<String, String> parameters) throws
            ProviderException {
        try {
            return( constructGenricResult(_bootImage.addBootImageProfile(parameters)));
        } catch (DataStoreException e) {
            throw new ProviderException(e.getMessage());
        }
    }

    PropertyMap editBootImageProfile(Map<String, String> parameters) throws
            ProviderException {
        try {
            return( constructGenricResult(_bootImage.editBootImageProfile(parameters)));
        } catch (DataStoreException e) {
            throw new ProviderException(e.getMessage());
        }
    }

    PropertyMap deleteBootImageProfile(String profileId) throws
            ProviderException {
        try {
            List<String> profileIds = _bootImage.listBootImageProfiles();
            if(!profileIds.contains(profileId)) {
                throw new ProviderException("Profile Id " + profileId + " doesn't exist");
            }
            return( constructGenricResult(_bootImage.deleteBootImageProfile(profileId)));
        } catch (DataStoreException e) {
            throw new ProviderException(e.getMessage());
        }
    }

    PropertyMap listBootImageProfiles() throws
            ProviderException {
        PropertyMap result = new PropertyMap();
        List<String> profiles;
        try {
            profiles = _bootImage.listBootImageProfiles();
        } catch (DataStoreException e) {
            throw new ProviderException(e.getMessage());
        }
        result.put("result-data-columns" ,1);
        result.put("result-data-lines" , profiles.size());
        result.put("result-status-code" ,0);
        result.put("schema", constructSingleMessageSchema("Profiles"));
        result.put("data", constructSingleMessageData(profiles));
        return result;
    }

    private PropertyArray constructBootImageSchema() {
        PropertyArray schema = new PropertyArray();
        PropertyMap firstColumn = new PropertyMap();
        firstColumn.put("data", "key");
        firstColumn.put("unit", "string");
        firstColumn.put("heading", "key");
        schema.add(firstColumn);
        PropertyMap secondColumn = new PropertyMap();
        secondColumn.put("data", "value");
        secondColumn.put("unit", "string");
        secondColumn.put("heading", "value");
        schema.add(secondColumn);
        return schema;
    }

    private PropertyArray extractValuesFromProfile(Map<String, String> bootImageInfo) {
        PropertyArray data = new PropertyArray();
        for (Map.Entry<String,String> entry : bootImageInfo.entrySet()) {
            PropertyArray result = new PropertyArray();
            result.add(entry.getKey());
            result.add(entry.getValue());
            data.add(result);
        }
        return data;
    }

    PropertyMap retrieveBootImageProfile(String[] profileIds) throws
            ProviderException {
        PropertyMap profilesInfo = new PropertyMap();
        for (String profileId : profileIds) {
            Map<String, String> bootImageInfo;
            PropertyMap profileInfo = new PropertyMap();
            try {
                bootImageInfo = _bootImage.retrieveBootImageProfile(profileId.trim());
            } catch (DataStoreException e) {
                throw new ProviderException(e.getMessage());            }
            if (bootImageInfo.size() == 0) {
                throw new ProviderException("Profile doesn't exist");
            }
            else {
                profileInfo.put("result-data-columns" ,2);
                profileInfo.put("result-data-lines" ,bootImageInfo.size());
                profileInfo.put("result-status-code" ,0);
                profileInfo.put("schema", constructBootImageSchema());
                profileInfo.put("data", extractValuesFromProfile(bootImageInfo));

            }
            profilesInfo.put(profileId, profileInfo);
        }
        return profilesInfo;
    }

    private BootImage _bootImage;
}
