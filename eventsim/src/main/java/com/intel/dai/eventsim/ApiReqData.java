package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.networking.HttpMethod;
import com.intel.networking.restserver.Request;
import com.intel.networking.restserver.RequestException;
import com.intel.networking.restserver.Response;
import com.intel.properties.PropertyMap;
import com.sun.istack.NotNull;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Description of class ApiReqData.
 * converts query payloads to map.
 * creates map with url,method and callback method.
 */
public class ApiReqData {

    ApiReqData(final Logger log) {
        log_ = log;
        parser_ = ConfigIOFactory.getInstance("json");
    }

    /**
     * This method calls respective callback method based on api request and sets the response.
     * @param request api request.
     * @param response response to api request.
     * @throws RequestException when unable to fetch http type method.
     */
    public void apiCallBack(final Request request, final Response response) throws RequestException, ConfigIOParseException {
        // Read the payload and create map with parameters
        Map<String, String> parameters = new HashMap<>();
        if (request.getMethod() == HttpMethod.POST) {
            parameters = convertHttpBodytoMap(request.getBody());
            String[] sub_component = request.getPath().split("/");
            parameters.put("sub_component", sub_component[sub_component.length - 1]);
        } else if (request.getMethod() == HttpMethod.GET || request.getMethod() == HttpMethod.DELETE) {
            parameters = convertHttpRequestToMap(request);
            String[] sub_component = request.getPath().split("/");
            parameters.put("sub_component", sub_component[sub_component.length - 1]);
        }

        //api request and http method it calls respective callback method
        NetworkSimulator callBack = callBackUrl(dispatchMap, request.getPath(), request.getMethod());
        if (callBack == null) {
            log_.warn("callback is empty");
            response.setCode(500);
            response.setBody(String.format("{\"reason\":\"No internal mapped callback method for '%s'\"}",
                    request.getPath()));
        } else {
            try {
                response.setBody(callBack.routeHandler(parameters));
                response.setCode(200);
            } catch (final SimulatorException e) {
                response.setCode(500);
                response.setBody(e.getMessage());
            } catch (final ResultOutputException e) {
                String result[] = e.getMessage().split("::");
                response.setCode(Integer.parseInt(result[0]));
                response.setBody(result[1]);
            }
        }
    }

    /**
     * This method is used to register uri and http method
     *
     * @param urlMethodObj map with url and method.
     * @param callBack callback method to requested api.
     * @throws SimulatorException this method requires input with non null values.
     */
    public void registerPathCallBack(@NotNull final PropertyMap urlMethodObj, @NotNull final NetworkSimulator callBack) throws SimulatorException {
        if (urlMethodObj == null || callBack == null)
            throw new SimulatorException("Could not register API URL or call back method : NULL value(s)");
        dispatchMap.put(urlMethodObj, callBack);
    }

    /**
     * This method is used to find the callback method
     *
     * @param dispatchMap map with uri and http methods.
     * @param path uri of requested api.
     * @param method http method of requested api.
     * @return callback method of requested api.
     */
    private NetworkSimulator callBackUrl(Map<PropertyMap, NetworkSimulator> dispatchMap, String path, HttpMethod method) {
        if (path == null || method == null)
            return null;
        PropertyMap urlMethod = new PropertyMap();
        urlMethod.put(path, method);
        NetworkSimulator callBackUrl = dispatchMap.getOrDefault(urlMethod, null);
        if (callBackUrl == null) {
            int lastOcrdIndex = path.lastIndexOf('/');
            String newUrl = path.substring(0, lastOcrdIndex + 1) + '*';
            PropertyMap obj = new PropertyMap();
            obj.put(newUrl, method);
            return dispatchMap.getOrDefault(obj, null);
        }
        return callBackUrl;
    }

    /**
     * This method is used to convert post payload parameters to map
     *
     * @param payload api payload.
     * @return map with payload parameters.
     * @throws ConfigIOParseException invalid json format payload.
     */
    private Map<String, String> convertHttpBodytoMap(String payload) throws ConfigIOParseException {
        /* Convert request body which is name value pair to a Map */
        if (payload.isEmpty())
            return new HashMap<>();
        PropertyMap payloadParameters = parser_.fromString(payload).getAsMap();
        Map<String, String> payLoadMap = new HashMap<>();
        for (Map.Entry<String, Object> parameter : payloadParameters.entrySet()) {
            String paramKey = parameter.getKey();
            Object paramValue = parameter.getValue();
            if (paramValue == null)
                payLoadMap.put(paramKey, null);
            else
                payLoadMap.put(paramKey, paramValue.toString());
        }
        return payLoadMap;
    }

    /**
     * This method is used to convert post payload parameters to map
     *
     * @param request api request.
     * @return map with payload parameters.
     * @throws RequestException unable to fetch get method query parameters.
     */
    private Map<String, String> convertHttpRequestToMap(Request request) throws RequestException {
        //Convert request query parameters to a Map
        Map<String, String> payLoadMap = new HashMap<>();
        String query = request.getQuery();
        String queryParameters = null;
        if (query != null && !query.isEmpty())
            queryParameters = URLDecoder.decode(query, StandardCharsets.UTF_8);
        if (queryParameters == null || queryParameters.isEmpty())
            return payLoadMap;
        String[] parts = queryParameters.split("&");
        for (String part : parts) {
            String[] keyValue = part.split("=");
            payLoadMap.put(keyValue[0], keyValue[1]);
        }
        return payLoadMap;
    }

    private final Logger log_;
    private Map<PropertyMap, NetworkSimulator> dispatchMap = new HashMap<>();
    private ConfigIO parser_;
}