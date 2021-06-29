package com.intel.dai.inventory.api.es;

import com.google.gson.Gson;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.HWInvDbApi;
import com.intel.dai.dsapi.pojo.*;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.inventory.ProviderInventoryNetworkForeignBus;
import com.intel.logging.Logger;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.voltdb.client.ProcCallException;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.util.function.Consumer;

class ElasticsearchIndexIngester {
    private final Logger log_;
    private final static Gson gson = new Gson();
//    private static VoltdbClient dbClient = null;
    private final String index;   // Elasticsearch index being ingested into voltdb
    private final Scroll scroll;  // defines scroll characteristics, such as minutes to keep scroll control structure alive
    private final RestHighLevelClient esClient;
    private final Consumer<ImmutablePair<String, String>> ingestMethodReference;
    protected HWInvDbApi onlineInventoryDatabaseClient_;                // voltdb
    private SearchRequest searchRequest;
    private SearchResponse searchResponse;
    private SearchHit[] searchHits;
    private String scrollId;    // think of this as a cursor marking the lower edge of iterated json documents
    private int totalNumberOfDocumentsEnumerated = 0;

    ElasticsearchIndexIngester(RestHighLevelClient elasticsearchHighLevelClient, String elasticsearchIndex,
                               NodeInventoryIngestor nodeInventoryIngestor, Logger log) {
        log_ = log;
        index = elasticsearchIndex;
        esClient = elasticsearchHighLevelClient;
        scroll = getScroll();
        switch (index) {
            case "kafka_dimm":
                ingestMethodReference = this::ingestDimm;
                break;
            case "kafka_fru_host":
                ingestMethodReference = this::ingestFruHost;
                break;
            default:
                ingestMethodReference = null;
        }

        final DataStoreFactory factory_ = ProviderInventoryNetworkForeignBus.getDataStoreFactory();
        if (factory_ == null) {
            log_.error("ProviderInventoryNetworkForeignBus.getDataStoreFactory() => null");
            return;
        }
        onlineInventoryDatabaseClient_ = factory_.createHWInvApi();
    }

    private void ingestFruHost(ImmutablePair<String, String> doc) {
        String id = doc.left;
        FruHost fruHost = gson.fromJson(doc.right, FruHost.class);

        fruHost.oob_fru = gson.fromJson(fruHost.raw_OOB_FRU, OOB_FRU_POJO.class);
        fruHost.raw_OOB_FRU = null;
        fruHost.oob_rev_info = gson.fromJson(fruHost.raw_OOB_REV_INFO, OOB_REV_INFO_POJO.class);
        fruHost.raw_OOB_REV_INFO = null;

        fruHost.ib_bios = gson.fromJson(fruHost.raw_IB_BIOS, IB_BIOS_POJO.class);
        fruHost.raw_IB_BIOS = null;

        fruHost.boardSerial = fruHost.oob_fru.Board_Serial;

        try {
            onlineInventoryDatabaseClient_.ingest(id, fruHost);
        } catch (DataStoreException e) {
            System.out.printf("DataStoreException: %s%n", e.getMessage());
        }
    }

    private void ingestDimm(ImmutablePair<String, String> doc) {
        String id = doc.left;
        Dimm dimm = gson.fromJson(doc.right, Dimm.class);
        dimm.ib_dimm = gson.fromJson(dimm.raw_IB_DIMM, IB_DIMM_POJO.class);
        dimm.raw_IB_DIMM = null;
        dimm.locator = dimm.ib_dimm.Locator;

        try {
            onlineInventoryDatabaseClient_.ingest(id, dimm);
        } catch (DataStoreException e) {
            System.out.printf("DataStoreException: %s%n", e.getMessage());
        }
    }

    boolean ingestIndexIntoVoltdb() throws IOException {

        System.out.println("getChronologicalSearchRequest ...");
        getChronologicalSearchRequest();
        getFirstScrollSearchResponse();

        while (isAnyHitInScrollSearchResponse()) {
            ingestScrollSearchResponseIntoVoltdb();
            getNextScrollWindow();
        }

        return clearScroll();
    }

    long getNumberOfDocumentsEnumerated() {
        return totalNumberOfDocumentsEnumerated;
    }

    private Scroll getScroll() {
        long numberOfMinutesToKeepScrollAlive = 1L;
        return new Scroll(TimeValue.timeValueMinutes(numberOfMinutesToKeepScrollAlive));
    }

    private boolean clearScroll() throws IOException {
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        ClearScrollResponse clearScrollResponse = esClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        return clearScrollResponse.isSucceeded();
    }

    private void ingestScrollSearchResponseIntoVoltdb() {
        for (SearchHit hit : searchHits) {
//            String source = hit.getSourceAsString();
//            String id = hit.getId();
            ImmutablePair<String, String> doc = new ImmutablePair<>(hit.getId(), hit.getSourceAsString());
//            System.out.println(str);
            totalNumberOfDocumentsEnumerated += 1;
            ingestMethodReference.accept(doc);
        }
    }

    private boolean isAnyHitInScrollSearchResponse() {
        return searchHits != null && searchHits.length > 0;
    }

    /**
     * The search request corresponds to the search DSL json.  It is useful to think of this method as
     * construct this query json.
     */
    private void getChronologicalSearchRequest() {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        int resultSetSize = 100;
        String timestamp = "timestamp";
        searchSourceBuilder.query(QueryBuilders.matchAllQuery()).sort(timestamp).size(resultSetSize);
        searchRequest = new SearchRequest(index).source(searchSourceBuilder).scroll(scroll);
    }

    private void getFirstScrollSearchResponse() throws IOException {
        searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);
        updateScrollWindow();
    }

    private void updateScrollWindow() {
        scrollId = searchResponse.getScrollId();
        searchHits = searchResponse.getHits().getHits();
    }

    private void getNextScrollWindow() throws IOException {
        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId).scroll(scroll);
        searchResponse = esClient.scroll(scrollRequest, RequestOptions.DEFAULT);
        updateScrollWindow();
    }
}
