package com.intel.dai.inventory.api.es

import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.HWInvUtil
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl
import com.intel.dai.dsimpl.voltdb.VoltHWInvDbApi
import com.intel.dai.exceptions.DataStoreException
import com.intel.dai.inventory.utilities.Helper
import com.intel.logging.Logger
import org.elasticsearch.client.RestHighLevelClient
import spock.lang.Specification

class ElasticsearchIndexIngesterITSpec extends Specification {
    Elasticsearch es = new Elasticsearch()
    RestHighLevelClient esClient
    ElasticsearchIndexIngester ts
    HWInvUtil util = new HWInvUtilImpl(Mock(Logger))
    Logger logger = Mock(Logger)
    DataStoreFactory dsClientFactory = Mock(DataStoreFactory)

    String[] servers = ['css-centos-8-00.ra.intel.com']

    def setup() {
        println Helper.testStartMessage(specificationContext)
        "echo 'truncate table raw_DIMM;' | sqlcmd --servers=css-centos-8-00.ra.intel.com".execute().text
        "echo 'truncate table raw_FRU_Host;' | sqlcmd --servers=css-centos-8-00.ra.intel.com".execute().text

        esClient = es.getRestHighLevelClient("cmcheung-centos-7.ra.intel.com", 9200,
                "elkrest", "elkdefault")
        dsClientFactory.createHWInvApi() >> new VoltHWInvDbApi(logger, util, servers)
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
        ts.ingestIndexIntoVoltdb() == status
        ts.getNumberOfDocumentsEnumerated() > 0
        ts.getTotalNumberOfDocumentsIngested() > 0
        ts.getNumberOfDocumentsEnumerated() == ts.getTotalNumberOfDocumentsIngested()

        where:
        elasticsearchIndex  || status
        "kafka_fru_host"    || true
        "kafka_dimm"        || true
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
        "echo 'truncate table raw_DIMM;' | sqlcmd --servers=css-centos-8-00.ra.intel.com".execute().text
        "echo 'truncate table raw_FRU_Host;' | sqlcmd --servers=css-centos-8-00.ra.intel.com".execute().text
        "echo 'truncate table Node_Inventory_History;' | sqlcmd --servers=css-centos-8-00.ra.intel.com".execute().text
        dsClientFactory.createHWInvApi() >> new VoltHWInvDbApi(logger, util, servers)
        ts = new NodeInventoryIngester(dsClientFactory, logger)

        def es = new Elasticsearch()
        def esClient = es.getRestHighLevelClient("cmcheung-centos-7.ra.intel.com", 9200,
                "elkrest", "elkdefault")

        for (String elasticsearchIndex in ['kafka_fru_host', 'kafka_dimm']) {
            def eii = new ElasticsearchIndexIngester(esClient, elasticsearchIndex, dsClientFactory, logger)
            eii.ingestIndexIntoVoltdb()
        }
    }

    def cleanup() {
        println Helper.testEndMessage(specificationContext)
    }

    def "NodeInventoryIngester constructor - positive"() {
        expect: new NodeInventoryIngester(dsClientFactory, logger) != null
    }

    def "ingestInitialNodeInventoryHistory - positive"() {
        when: ts.ingestInitialNodeInventoryHistory()
        then: ts.getNumberNodeInventoryJsonIngested() > 0
    }
}
