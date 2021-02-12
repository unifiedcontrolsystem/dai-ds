package com.intel.dai.provisioners;

import com.intel.authentication.TokenAuthentication;
import com.intel.authentication.TokenAuthenticationException;
import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.*;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.dai.network_listener.DataType;
import com.intel.logging.Logger;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restclient.RESTClientFactory;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.intel.runtime_utils.TimeUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Description of class NetworkAdapterProvisionerBase.
 * This class is used to fetch initial node state before the startup of adapter.
 */
public class AdapterProvisionerApi {

    public AdapterProvisionerApi(DataStoreFactory factory, AdapterInformation adapter, Logger log) {
        factory_ = factory;
        adapter_ = adapter;
        log_ = log;
        operations_ = factory_.createAdapterOperations(adapter_);
        eventActions_ = factory_.createRasEventLog(adapter_);
        nodeInfo_ = factory_.createNodeInformation();
        parser_ = ConfigIOFactory.getInstance("json");
   }

    /**
     * This method is used to get api url information from configuration file
     * @param apiConfig data from configuration file
     */
    public void initialise(PropertyMap apiConfig) {
        try {
            apiConfig_ = apiConfig;
            nodeStateInfoUrl_ = apiConfig_.getStringOrDefault("nodeStateInfoUrl", nodeStateInfoUrl_);
            nodeStateForLocationInfoUrl_ = apiConfig_.getStringOrDefault("nodeStateForLocationInfoUrl", nodeStateForLocationInfoUrl_);
            informWlm_ = apiConfig_.getBooleanOrDefault("informWorkLoadManager", informWlm_);
            updateInitialNodeStates();
        } catch (PropertyNotExpectedType | DataStoreException | ConversionException e) {
            log_.error(e.getMessage());
        }
    }

    /**
     * This method is used to update node states from api data.
     * @throws DataStoreException unable to store data into db
     * @throws ConversionException unable to store data into db
     * @throws PropertyNotExpectedType unable to store data into db
     */
    private void updateInitialNodeStates() throws DataStoreException, ConversionException, PropertyNotExpectedType {
        fetchNodeLocations();
        List<CommonDataFormat> dataList = fetchInitialNodeStates();
        if(dataList != null) {
            log_.debug("Performing initial node state update actions...");
            for (CommonDataFormat data : dataList) {
                long dataTimestamp = data.getNanoSecondTimestamp();
                if(nodeLocations_.contains(data.getLocation()) && data.getStateEvent() != null)
                    operations_.markNodeState(data.getStateEvent(), data.getLocation(), dataTimestamp, informWlm_);
            }
        }
    }

    /**
     * This method is used to fetch actual and foreign locations
     * @throws DataStoreException unable to fetch locations data from db
     */
    private void fetchNodeLocations() throws DataStoreException {
        loadNodeLocations();
        loadForeignLocations();
    }

    /**
     * This method is used to fetch Foreign API node states data
     * @return node locations, and their respective current states
     * @throws PropertyNotExpectedType unable to fetch data
     * @throws ConversionException unable to fetch data
     */
    private List<CommonDataFormat> fetchInitialNodeStates() throws PropertyNotExpectedType, ConversionException {
        //send request for node states and update node state
        List<CommonDataFormat> results = new ArrayList<>();

        log_.info("Sending Node states URL request = %s", nodeStateInfoUrl_);
        PropertyDocument bootParametersForLocationApiData = sendForeignApiRequest(nodeStateInfoUrl_);
        if(bootParametersForLocationApiData == null) {
            return new ArrayList<>();
        }
        PropertyMap nodeStatesMap = bootParametersForLocationApiData.getAsMap();
        if(!nodeStatesMap.containsKey(NODE_STATE_COMPONENTS)) {
            log_.error("'Component' parameter is missing in api data");
            return new ArrayList<>();
        }

        PropertyArray nodeStateArray = nodeStatesMap.getArrayOrDefault(NODE_STATE_COMPONENTS, new PropertyArray());
        if(!nodeStateArray.isEmpty()) {
            for(Object obj : nodeStateArray) {
                PropertyMap nodeState = (PropertyMap) obj;
                if(!nodeState.containsKey(NODE_ID)) {
                    log_.warn("'" + NODE_ID + "'is missing in api data : " + nodeState);
                    continue;
                }
                if(!nodeState.containsKey(NODE_STATE)) {
                    log_.warn("'" + NODE_STATE + "'is missing in api data : " + nodeState);
                    continue;
                }

                String foreignLocation = nodeState.getString(NODE_ID);
                if(!foreignLocations_.contains(foreignLocation)) {
                    log_.warn("'" + foreignLocation + "' foreign location does not have valid location : " + nodeState);
                    continue;
                }

                String state = nodeState.getString(NODE_STATE);
                if(!conversionMap_.containsKey(state)) {
                    log_.warn("'" + state + "' state does not have mapped state : " + nodeState);
                    continue;
                }

                String location = CommonFunctions.convertForeignToLocation(foreignLocation);
                BootState currentState = conversionMap_.get(state);

                long usTimestamp = TimeUtils.nanosecondsToMicroseconds(TimeUtils.getNsTimestamp());
                CommonDataFormat common = new CommonDataFormat(usTimestamp, location, DataType.InitialNodeStateData);
                common.setStateChangeEvent(currentState);
                common.storeExtraData(ORIG_FOREIGN_LOCATION_KEY, foreignLocation);
                results.add(common);
            }
        }
        return results;
    }

    /**
     * Thsi method is used to send ForeignAPI request to ftech data
     * @param url Foreign API url
     * @return Foreign API url data
     */
    private PropertyDocument sendForeignApiRequest(String url) {
        String requestedUrl = "";
        try {

            if(client_ == null) {
                client_ = createClient();
                if (client_ == null)
                    throw new RESTClientException("Failed to get the REST client implementation");
                createTokenProvider();
            }

            URI apiReqURI = makeUri(url);
            requestedUrl = apiReqURI.toURL().toString();
            log_.info("URL= %s", requestedUrl);
            BlockingResult result = client_.getRESTRequestBlocking(apiReqURI);

            if(result.code != 200) {
                throw new RESTClientException("RESTClient resulted in HTTP code: " + result.code +
                        ":" + result.responseDocument);
            }
            return parser_.fromString(result.responseDocument);

        } catch(RESTClientException | ConfigIOParseException | MalformedURLException | URISyntaxException e) {
            log_.exception(e);
            long ts = TimeUtils.nanosecondsToMicroseconds(TimeUtils.getNsTimestamp());
            eventActions_.logRasEventNoEffectedJob("1000000085", String.format("Full URL=%s", requestedUrl),
                    null, ts, adapter_.getType(), adapter_.getBaseWorkItemId());
        }
        return null;
    }

    /**
     * This method is used to create URL object using url
     * @param uri object used to send get API request
     * @return URI object
     * @throws URISyntaxException unable to create URI object
     */
    private URI makeUri(String uri) throws URISyntaxException {
        return new URI(String.format("%s", uri));
    }

    /**
     * Thsi method is used to create rest client
     * @return rest client
     * @throws RESTClientException unable to create rest client
     */
    protected RESTClient createClient() throws RESTClientException { // protected for test mocking.
        return RESTClientFactory.getInstance("apache", log_);
    }

    /**
     * This method is used to create token provider
     */
    private void createTokenProvider() {
        if (apiConfig_.getStringOrDefault("tokenAuthProvider", null) != null) {
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<String, Object> entry : apiConfig_.entrySet()) {
                log_.debug("REST Client args: %s = %s", entry.getKey(), entry.getValue().toString());
                map.put(entry.getKey(), entry.getValue().toString());
            }
            createTokenProvider(map.get("tokenAuthProvider"), map);
            client_.setTokenOAuthRetriever(tokenProvider_);
        }
    }

    /**
     * This method is used to create token authentication constructor
     */
    protected TokenAuthentication callTokenConstructor(Constructor<?> ctor) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        return (TokenAuthentication) ctor.newInstance();
    }

    /**
     * This method is used to create token provider
     */
    private void createTokenProvider(String className, Map<String,String> config) {
        try {
            Class<?> classObj = Class.forName(className);
            Constructor<?> ctor = classObj.getDeclaredConstructor();
            tokenProvider_ = callTokenConstructor(ctor);
            tokenProvider_.initialize(log_, config);
        } catch(ClassNotFoundException e) {
            log_.exception(e, String.format("Missing TokenAuthentication implementation '%s'", className));
        } catch (NoSuchMethodException e) {
            log_.exception(e, String.format("Missing public constructor for TokenAuthentication implementation '%s'",
                    className));
        } catch (IllegalAccessException e) {
            log_.exception(e, String.format("Default constructor for TokenAuthentication implementation " +
                    "'%s' must be public", className));
        } catch (InstantiationException | InvocationTargetException | TokenAuthenticationException e) {
            log_.exception(e, String.format("Cannot construct TokenAuthentication implementation '%s'", className));
        }
    }

    /**
     * This method is used to load all node locations.
     * @throws DataStoreException unable to fetch data
     */
    private void loadNodeLocations() throws DataStoreException {
        nodeLocations_ = nodeInfo_.getNodeLocations();
    }

    /**
     * This method is used to load all node locations.
     * @throws DataStoreException unable to fetch data
     */
    private void loadForeignLocations() {
        foreignLocations_ = CommonFunctions.getForeignLocations();
    }

    private boolean informWlm_ = false;

    private RESTClient client_ = null;
    private TokenAuthentication tokenProvider_ = null;
    private NodeInformation nodeInfo_;

    private PropertyMap apiConfig_ = new PropertyMap();

    private List<String> nodeLocations_;
    private Set<String> foreignLocations_;

    private String nodeStateInfoUrl_ = "/apis/smd/hsm/v1/State/Components";
    private String nodeStateForLocationInfoUrl_ = "/apis/smd/hsm/v1/State/Components/";

    final private static String NODE_ID = "ID";
    final private static String NODE_STATE_COMPONENTS = "Components";
    final private static String NODE_STATE = "State";
    private final static String ORIG_FOREIGN_LOCATION_KEY = "foreignLocation";

    private final ConfigIO parser_;
    private final DataStoreFactory factory_;
    private final AdapterInformation adapter_;
    private final Logger log_;
    private final AdapterOperations operations_;
    private final RasEventLog eventActions_;

    private final static Map<String, BootState> conversionMap_ = new HashMap<String, BootState>() {{
        put("Ready", BootState.NODE_ONLINE);
        put("Off", BootState.NODE_OFFLINE);
        put("Empty", BootState.EMPTY);
        put("On", BootState.NODE_BOOTING);
    }};
}
