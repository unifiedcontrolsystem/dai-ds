package com.intel.dai.provisioners;

import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.AdapterOperations;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.NodeInformation;
import com.intel.dai.dsapi.RasEventLog;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AdapterProvisionerApiTest {

   class TestAdapterProvisionerApi extends AdapterProvisionerApi {
        TestAdapterProvisionerApi(DataStoreFactory factory, AdapterInformation adapter, Logger log) {
            super(factory, adapter, log);
        }

        @Override protected RESTClient createClient() {
            return restClientMock_;
        }
    }

    @Before
    public void setUp() throws DataStoreException, RESTClientException {
        map_ = new PropertyMap();
        map_.put("informWorkLoadManager", true);
        map_.put("nodeStateInfoUrl", "http://localhost:1234/nodestate");
        map_.put("nodeStateForLocationInfoUrl", "http://localhost:1234/nodestateLocation");
        map_.put("tokenAuthProvider", "com.intel.authentication.KeycloakTokenAuthentication");

        List<String> locations = new ArrayList<>();
        locations.add("test-location-0");

        restClientMock_ = mock(RESTClient.class);
        factoryMock_ = mock(DataStoreFactory.class);
        adapterMock_ = mock(AdapterInformation.class);
        loggerMock_ = mock(Logger.class);
        operationsMock_ = mock(AdapterOperations.class);
        eventActionsMock_ = mock(RasEventLog.class);
        nodeInfoMock_ = mock(NodeInformation.class);


        when(factoryMock_.createAdapterOperations(adapterMock_)).thenReturn(operationsMock_);
        when(factoryMock_.createRasEventLog(adapterMock_)).thenReturn(eventActionsMock_);
        when(factoryMock_.createNodeInformation()).thenReturn(nodeInfoMock_);
        when(nodeInfoMock_.getNodeLocations()).thenReturn(locations);
        when(restClientMock_.getRESTRequestBlocking(any())).thenReturn(new BlockingResult(200,
                nodeStateMapStr_, null));

        doNothing().when(restClientMock_).setTokenOAuthRetriever(any());

        testAdapterProvisionerApi_ = new TestAdapterProvisionerApi(factoryMock_, adapterMock_, loggerMock_);
    }

    @Test
    public void initialise() {
        testAdapterProvisionerApi_.initialise(map_);
    }

    @Test
    public void createRestClientException() throws RESTClientException {
       restClientMock_ = null;
       testAdapterProvisionerApi_.initialise(map_);

       restClientMock_ = mock(RESTClient.class);
       when(restClientMock_.getRESTRequestBlocking(any())).thenReturn(new BlockingResult(400,
               nodeStateMapStr_, null));
       testAdapterProvisionerApi_.initialise(map_);
    }


    private RESTClient restClientMock_;

    private PropertyMap map_;

    private TestAdapterProvisionerApi testAdapterProvisionerApi_;
    private DataStoreFactory factoryMock_;
    private AdapterInformation adapterMock_;
    private Logger loggerMock_;
    private AdapterOperations operationsMock_;
    private RasEventLog eventActionsMock_;
    private NodeInformation nodeInfoMock_;
    private String nodeStateMapStr_ = "{\"Components\":[{\"ID\": \"xname-0\", \"State\": \"Ready\"}, {\"State\": \"Ready\"}, {\"ID\": \"xname-0\"}, {\"ID\": \"test\", \"State\": \"Ready\"}, {\"ID\": \"xname-0\", \"State\": \"Boot\"}]}";
}
