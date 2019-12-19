package com.intel.dai.populate

import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.LegacyVoltDbDirectAccess
import com.intel.logging.Logger
import com.intel.properties.PropertyArray
import com.intel.properties.PropertyMap
import org.voltdb.VoltTable
import org.voltdb.VoltType
import org.voltdb.client.Client
import org.voltdb.client.ClientResponse
import spock.lang.Specification

class DefaultOnlineTierDataLoaderSpec extends Specification {
    def logger_
    def client_
    def factory_
    def legacy_

    def underTest_
    void setup() {
        logger_ = Mock(Logger)
        client_ = Mock(Client)
        factory_ = Mock(DataStoreFactory)
        legacy_ = Mock(LegacyVoltDbDirectAccess)
        legacy_.getVoltDbClient() >> client_
        factory_.createVoltDbLegacyAccess() >> legacy_

        underTest_ = new DefaultOnlineTierDataLoader(logger_, factory_)
        underTest_.client_ = client_
    }

    def "Test internalCallProcedure"() {
        expect: underTest_.internalCallProcedure("testProcedure", "arg1", "arg2") == null
    }

    def "Test connectToVoltDB"() {
        expect: underTest_.connectToVoltDB() == client_
    }

    def "Test keywordSubstitutions"() {
        expect: underTest_.keywordSubstitutions(input, "/tmp/") == result

        where:
        input                   || result
        null                    || null
        "\$UCSLOGFILEDIRECTORY" || "/tmp/"
    }

    def "Test createHouseKeepingCallbackNoRtrnValue"() {
        expect: underTest_.createHouseKeepingCallbackNoRtrnValue("", "") != null
    }

    def "Test populateRasEventMetaData"() {
        underTest_.populateRasEventMetaData("../config-files/RasEventMetaData.json")
        expect: true
    }

    def "Test populateRasEventMetaData with exception"() {
        given:
        when: underTest_.populateRasEventMetaData("/tmp/somefile.json")
        then: thrown(RuntimeException)
    }

    def "Test fillInMachineCfgEntries"() {
        underTest_.fillInMachineCfgEntries(new PropertyMap(), "", false)
        expect: true
    }

    def "Test traverseToJsonDefinitionAndProcessItsContents"() {
        def key = "key"
        def top = new PropertyMap()
        def level2 = new PropertyMap()
        def content = new PropertyArray()
        level2.put("type", type)
        level2.put("content", useContent?content:null)
        top.put(key, setLevel2?level2:null)
        DefaultOnlineTierDataLoader.MachineConfigEntry entry = new DefaultOnlineTierDataLoader.MachineConfigEntry(
                "bmc", "ip", "bmcMac", "mac", "bmcHostname", "hostname", "bootId", "env", "aggregator"
        )
        underTest_.lctnToMchCfgMap_ = Mock(HashMap)
        underTest_.lctnToMchCfgMap_.get(_) >> entry
        underTest_.traverseToJsonDefinitionAndProcessItsContents(top, prev, "", key)

        expect: result

        where:
        prev       | setLevel2 | type           | useContent || result
        ""         | false     | ""             | true       || true
        "previous" | true      | null           | true       || true
        "previous" | true      | "Rack"         | true       || true
        "previous" | true      | "Chassis"      | true       || true
        "previous" | true      | "Switch"       | true       || true
        "previous" | true      | "ServiceNode"  | true       || true
        "previous" | true      | "ComputeNode"  | true       || true
        "previous" | true      | "SuperNode"    | true       || true
        "previous" | true      | "PowerSupply"  | true       || true
        "previous" | true      | "Fan"          | true       || true
        "previous" | true      | "PDU"          | true       || true
        "previous" | true      | "CDU"          | true       || true
        "previous" | true      | "ChilledDoor"  | true       || true
        "previous" | true      | "coolingTower" | true       || true
        "previous" | true      | "default"      | false      || true
    }

    def "Test MyCallbackForHouseKeepingNoRtrnValue.statusByteAsString"() {
        def object = new DefaultOnlineTierDataLoader.MyCallbackForHouseKeepingNoRtrnValue(underTest_, "", "")
        expect: object.statusByteAsString(input) == result
        where:
        input                             || result
        ClientResponse.USER_ABORT         || "USER_ABORT"
        ClientResponse.SUCCESS            || "SUCCESS"
        ClientResponse.CONNECTION_TIMEOUT || "CONNECTION_TIMEOUT"
        ClientResponse.CONNECTION_LOST    || "CONNECTION_LOST"
        ClientResponse.GRACEFUL_FAILURE   || "GRACEFUL_FAILURE"
        ClientResponse.RESPONSE_UNKNOWN   || "RESPONSE_UNKNOWN"
        ClientResponse.UNEXPECTED_FAILURE || "UNEXPECTED_FAILURE"
        ClientResponse.SERVER_UNAVAILABLE || ClientResponse.SERVER_UNAVAILABLE.toString()
    }

    def "Test MyCallbackForHouseKeepingNoRtrnValue.clientCallback"() {
        def response = Mock(ClientResponse)
        def object = new DefaultOnlineTierDataLoader.MyCallbackForHouseKeepingNoRtrnValue(underTest_, "", "")
        response.getStatus() >> status
        object.clientCallback(response)
        expect: result
        where:
        status                             || result
        ClientResponse.SUCCESS             || true
        ClientResponse.OPERATIONAL_FAILURE || true
    }

    def "Test compareManifest"() {
        DefaultOnlineTierDataLoader.ManifestContent a = new DefaultOnlineTierDataLoader.ManifestContent("a", "def")
        DefaultOnlineTierDataLoader.ManifestContent b = new DefaultOnlineTierDataLoader.ManifestContent("b", "def")
        expect: DefaultOnlineTierDataLoader.compareManifest(a, b) == -1
    }

    def "Test PropertyMapComparator.compare"() {
        PropertyMap a = new PropertyMap()
        PropertyMap b = new PropertyMap()
        a.put("name", "a")
        b.put("name", "b")
        def comparator = new DefaultOnlineTierDataLoader.PropertyMapComparator()
        expect: comparator.compare(a, b) == -1
    }

    def buildScalarResponse(long value, byte status) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        VoltTable table = new VoltTable(new VoltTable.ColumnInfo("value", VoltType.BIGINT))
        table.addRow(value)
        response.getResults() >> [ table ]
        return response
    }

    def buildUcsValueResponse(String value, byte status, boolean addRow) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        VoltTable table = new VoltTable(new VoltTable.ColumnInfo("Key", VoltType.STRING),
                new VoltTable.ColumnInfo("Value", VoltType.STRING))
        if(addRow)
            table.addRow("key", value)
        response.getResults() >> [ table ]
        return response
    }

    def "Test doPopulate"() {
        client_.callProcedure("ComputeNodeCount") >> buildScalarResponse(value, ClientResponse.SUCCESS)
        client_.callProcedure("UCSCONFIGVALUE.select", "UcsLogfileDirectory") >>
                buildUcsValueResponse("", ClientResponse.SUCCESS, true)
        expect: underTest_.doPopulate("localhost", "../config-files/SystemManifest.json",
                "../config-files/MachineConfig.json", "../config-files/RasEventMetaData.json") == 1
        where:
        value || result
        0     || 0
        1     || 1
    }
}
