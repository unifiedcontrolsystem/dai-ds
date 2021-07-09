// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api.es

import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.HWInvUtil
import com.intel.dai.dsapi.InventoryTrackingApi
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl
import com.intel.dai.dsimpl.voltdb.VoltHWInvDbApi
import com.intel.dai.exceptions.DataStoreException
import com.intel.dai.inventory.utilities.Helper
import com.intel.logging.Logger
import org.elasticsearch.client.RestHighLevelClient
import spock.lang.Specification

class ElasticsearchIndexIngesterITSpec extends Specification {
    Logger logger = Mock(Logger)
    Elasticsearch es = new Elasticsearch(logger)
    RestHighLevelClient esClient
    ElasticsearchIndexIngester ts
    HWInvUtil util = new HWInvUtilImpl(Mock(Logger))
    DataStoreFactory dsClientFactory = Mock(DataStoreFactory)
    String[] voltDbServers = ['css-centos-8-00.ra.intel.com']

    def setup() {
        println Helper.testStartMessage(specificationContext)
        "./src/integration/resources/scripts/drop_inventory_data.sh".execute().text

        esClient = es.getRestHighLevelClient("cmcheung-centos-7.ra.intel.com", 9200,
                "elkrest", "elkdefault")
        dsClientFactory.createHWInvApi() >> new VoltHWInvDbApi(logger, util, voltDbServers)
    }

    def cleanup() {
        println Helper.testEndMessage(specificationContext)
        esClient.close()
    }

    def "ElasticsearchIndexIngester constructor - positive"() {
        expect:
        new ElasticsearchIndexIngester(esClient, elasticsearchIndex, dsClientFactory, logger) != status

        where:
        elasticsearchIndex  || status
        "kafka_fru_host"    || null
        "kafka_dimm"        || null
    }

    def "ingestIndexIntoVoltdb - negative"() {
        ts = new ElasticsearchIndexIngester(esClient, elasticsearchIndex, dsClientFactory, logger)

        when:ts.ingestIndexIntoVoltdb()
        then: thrown DataStoreException

        where:
        elasticsearchIndex  || result
        "bad_index_name"    || "None"
    }

    def "ingestIndexIntoVoltdb - positive"() {
        ts = new ElasticsearchIndexIngester(esClient, elasticsearchIndex, dsClientFactory, logger)

        expect:
        ts.ingestIndexIntoVoltdb()
        ts.getNumberOfDocumentsEnumerated() == numberDocuments
        ts.getTotalNumberOfDocumentsIngested() == numberDocuments
        ts.getNumberOfDocumentsEnumerated() == ts.getTotalNumberOfDocumentsIngested()

        where:
        elasticsearchIndex  | numberDocuments
        "kafka_fru_host"    | 34
        "kafka_dimm"        | 243
    }
}


class NodeInventoryIngesterITSpec extends Specification {
    NodeInventoryIngester ts
    HWInvUtil util = new HWInvUtilImpl(Mock(Logger))
    Logger logger = Mock(Logger)
    DataStoreFactory dsClientFactory = Mock(DataStoreFactory)
    String[] servers = ['css-centos-8-00.ra.intel.com']

    def setup() {
        println Helper.testStartMessage(specificationContext)
        "./src/integration/resources/scripts/drop_inventory_data.sh".execute().text

        dsClientFactory.createHWInvApi() >> new VoltHWInvDbApi(logger, util, servers)
        dsClientFactory.createInventoryTrackingApi() >> Mock(InventoryTrackingApi)

        ts = new NodeInventoryIngester(dsClientFactory, logger)

        def es = new Elasticsearch(logger)
        def esClient = es.getRestHighLevelClient("cmcheung-centos-7.ra.intel.com", 9200,
                "elkrest", "elkdefault")

        for (String elasticsearchIndex in ['kafka_fru_host', 'kafka_dimm']) {
            def eii = new ElasticsearchIndexIngester(esClient, elasticsearchIndex, dsClientFactory, logger)
            eii.ingestIndexIntoVoltdb()
        }

        es.close()
    }

    def cleanup() {
        println Helper.testEndMessage(specificationContext)
    }

    def "NodeInventoryIngester constructor - positive"() {
        expect: new NodeInventoryIngester(dsClientFactory, logger) != null
    }

    def "ingestInitialNodeInventoryHistory - positive"() {
        when: ts.ingestInitialNodeInventoryHistory()
        then: ts.getNumberNodeInventoryJsonIngested() == 8
    }
}
