// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api.es;

import com.google.gson.Gson;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.HWInvDbApi;
import com.intel.dai.dsapi.pojo.*;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.function.Consumer;

public class ElasticsearchIndexIngester {
    private final static Gson gson = new Gson();
    private final Logger log_;
    private final String index;   // Elasticsearch index being ingested into voltdb
    private final Scroll scroll;  // defines scroll characteristics, such as minutes to keep scroll control structure alive
    private final RestHighLevelClient esClient;
    private final Consumer<ImmutablePair<String, String>> ingestMethodReference;
    protected HWInvDbApi onlineInventoryDatabaseClient_;                // voltdb
    private SearchRequest searchRequest;
    private SearchResponse searchResponse;
    private SearchHit[] searchHits;
    private String scrollId;    // think of this as a cursor marking the lower edge of iterated json documents
    private long totalNumberOfDocumentsEnumerated = 0;
    private long totalNumberOfDocumentsIngested = 0;

    public ElasticsearchIndexIngester(RestHighLevelClient elasticsearchHighLevelClient, String elasticsearchIndex,
                                      DataStoreFactory factory, Logger log) {
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

        onlineInventoryDatabaseClient_ = factory.createHWInvApi();
        onlineInventoryDatabaseClient_.initialize();
    }

    public boolean ingestIndexIntoVoltdb() throws DataStoreException {

        log_.info("Starting getChronologicalSearchRequest %s ...", index);
        try {
            getChronologicalSearchRequest();
            getFirstScrollSearchResponse();

            while (isAnyHitInScrollSearchResponse()) {
                ingestScrollSearchResponseIntoVoltdb();
                getNextScrollWindow();
            }

            log_.info("Finished getChronologicalSearchRequest %s", index);
            return clearScroll();
        } catch (IOException e) {
            log_.error("IOException: %s", e.getMessage());
            throw new DataStoreException(e.getMessage());
        } catch (ElasticsearchStatusException e) {
            log_.error("ElasticsearchStatusException: %s", e.getMessage());
            throw new DataStoreException(e.getMessage());
        }
    }

    private void ingestFruHost(ImmutablePair<String, String> doc) {
        String id = doc.left;
        FruHost fruHost = gson.fromJson(doc.right, FruHost.class);

        fruHost.oob_fru = gson.fromJson(fruHost.rawOobFru, OobFruPojo.class);
        fruHost.rawOobFru = null;
        fruHost.oob_rev_info = gson.fromJson(fruHost.rawOobRevInfo, OobRevInfoPojo.class);
        fruHost.rawOobRevInfo = null;

        fruHost.ib_bios = gson.fromJson(fruHost.rawIbBios, IbBiosPojo.class);
        fruHost.rawIbBios = null;

        fruHost.boardSerial = fruHost.oob_fru.Board_Serial;

        try {
            totalNumberOfDocumentsIngested += onlineInventoryDatabaseClient_.ingest(id, fruHost);
        } catch (DataStoreException e) {
            log_.error("DataStoreException: %s", e.getMessage());
        }
    }

    private void ingestDimm(ImmutablePair<String, String> doc) {
        String id = doc.left;
        Dimm dimm = gson.fromJson(doc.right, Dimm.class);
        dimm.ib_dimm = gson.fromJson(dimm.rawIbDimm, IbDimmPojo.class);
        dimm.rawIbDimm = null;
        dimm.locator = dimm.ib_dimm.Locator;

        try {
            totalNumberOfDocumentsIngested += onlineInventoryDatabaseClient_.ingest(id, dimm);
        } catch (DataStoreException e) {
            log_.error("DataStoreException: %s", e.getMessage());
        }
    }

    public long getNumberOfDocumentsEnumerated() {
        return totalNumberOfDocumentsEnumerated;
    }

    public long getTotalNumberOfDocumentsIngested() {
        return totalNumberOfDocumentsIngested;
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
            ImmutablePair<String, String> doc = new ImmutablePair<>(hit.getId(), hit.getSourceAsString());
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
        String insertionOrder = "_doc";
        searchSourceBuilder.query(QueryBuilders.matchAllQuery()).sort(insertionOrder, SortOrder.ASC).size(resultSetSize);
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
