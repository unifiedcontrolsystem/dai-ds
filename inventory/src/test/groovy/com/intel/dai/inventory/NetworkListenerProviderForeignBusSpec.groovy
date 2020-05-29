package com.intel.dai.inventory

import com.intel.dai.dsapi.BootState
import com.intel.dai.network_listener.CommonDataFormat
import com.intel.dai.network_listener.DataType
import com.intel.dai.network_listener.NetworkListenerConfig
import com.intel.dai.network_listener.NetworkListenerProviderException
import com.intel.dai.network_listener.SystemActions
import com.intel.logging.Logger
import spock.lang.Specification

class NetworkListenerProviderForeignBusSpec extends Specification {
    def underTest_
    void setup() {
        underTest_ = new NetworkListenerProviderForeignBus(Mock(Logger))
    }

    def "processRawStringData - empty SCN"() {
        when: underTest_.processRawStringData(scnJson, Mock(NetworkListenerConfig))
        then: thrown exception

        where:
        scnJson     || exception
        null        || NetworkListenerProviderException
        ""          || NetworkListenerProviderException
        "{}"        || NetworkListenerProviderException
    }

    def "processRawStringData - bad SCN"() {
        when: underTest_.processRawStringData(scnJson, Mock(NetworkListenerConfig))
        then: thrown exception

        where:
        scnJson || exception
        null    || NetworkListenerProviderException
        ""      || NetworkListenerProviderException
        '{'     || NetworkListenerProviderException
        '['     || NetworkListenerProviderException
        '[]'    || NetworkListenerProviderException
        "cow"   || NetworkListenerProviderException
    }

    def "processRawStringData - edge cases - no or empty component list"() {
        when: underTest_.processRawStringData(scnJson, Mock(NetworkListenerConfig))
        then: thrown exception

        where:
        scnJson                             || exception
        "{}"                                || NetworkListenerProviderException
        '{Components: [], State: "Cow"}'    || NetworkListenerProviderException
    }

    def "processRawStringData - edge cases - unknown nodes"() {
        when: underTest_.processRawStringData(scnJson, Mock(NetworkListenerConfig))
        then: thrown exception

        where:
        scnJson                                 || exception
        '{Components: [""], State: "On"}'       || NetworkListenerProviderException
        '{Components: ["cow"], State: "On"}'    || NetworkListenerProviderException
        '{Components: ["", ""], State: "On"}'   || NetworkListenerProviderException
    }

    def "processRawStringData - unsupported boot state"() {
        when: underTest_.processRawStringData(scnJson, Mock(NetworkListenerConfig))
        then: notThrown exception

        where:
        scnJson                                                     || exception
        '{Components: ["x0c0s21b0n0"], State: "Populated"}'         || NetworkListenerProviderException
        '{Components: ["x0c0s21b0n0"], State: "Empty"}'             || NetworkListenerProviderException
    }

    def "processRawStringData - erroneous boot state"() {
        when: underTest_.processRawStringData(scnJson, Mock(NetworkListenerConfig))
        then: thrown exception

        where:
        scnJson                                                     || exception
        '{Components: ["x0c0s21b0n0"], State: ""}'                  || NetworkListenerProviderException
        '{Components: ["x0c0s21b0n0"], State: "Chicken"}'           || NetworkListenerProviderException
        '{Components: ["x0c0s21b0n0"], State: "Cow"}'               || NetworkListenerProviderException
    }
    def "processRawStringData - common usages - sizes"() {
        expect: underTest_.processRawStringData(scnJson, Mock(NetworkListenerConfig)).size() == size

        where:
        scnJson                                                     || size
        '{Components: [], State: "On"}'                             || 0
        '{Components: ["x0c0s21b0n0"], State: "Off"}'               || 1
        '{Components: ["x0c0s21b0n0", "x0c0s24b0n0"], State: "On"}' || 2
    }

    def "processRawStringData - common usages - data"() {
        given:
        def scnJson = '{Components: ["x0c0s21b0n0", "x0c0s24b0n0"], State: "On"}'

        when:
        def cdfs = underTest_.processRawStringData(scnJson, Mock(NetworkListenerConfig))
        then:
        cdfs[0].getLocation() == "R0-CH0-CB0-CN3"
        cdfs[0].getDataType() == DataType.InventoryChangeEvent
        cdfs[0].getStateEvent() == BootState.NODE_ONLINE
        cdfs[0].retrieveExtraData('foreignLocationKey') == 'x0c0s21b0n0'

        cdfs[1].getLocation() == "R0-CH0-CB0-CN2"
        cdfs[1].getDataType() == DataType.InventoryChangeEvent
        cdfs[1].getStateEvent() == cdfs[0].getStateEvent()
        cdfs[1].retrieveExtraData('foreignLocationKey') == 'x0c0s24b0n0'

        cdfs[0].getNanoSecondTimestamp() == cdfs[1].getNanoSecondTimestamp()
    }

    def "Test empty actOnData for on event"() {
        def data = new CommonDataFormat(0L,"location", DataType.InventoryChangeEvent)
        data.setStateChangeEvent(BootState.NODE_ONLINE)
        data.storeExtraData("foreignLocationKey", "x0")
        def sa = Mock(SystemActions)
        sa.isHWInventoryEmpty() >> true
        underTest_.actOnData(data, Mock(NetworkListenerConfig), sa)
        expect: true
    }

    def "Test empty actOnData for off event"() {
        def data = new CommonDataFormat(0L,"location", DataType.InventoryChangeEvent)
        data.setStateChangeEvent(BootState.NODE_OFFLINE)
        data.storeExtraData("foreignLocationKey", "x0")
        def sa = Mock(SystemActions)
        sa.isHWInventoryEmpty() >> true
        underTest_.actOnData(data, Mock(NetworkListenerConfig), sa)
        expect: true
    }

    def "Test empty actOnData for unknown event"() {
        def data = new CommonDataFormat(0L,"location", DataType.InventoryChangeEvent)
        data.setStateChangeEvent(BootState.NODE_BOOTING)
        data.storeExtraData("foreignLocationKey", "x0")
        def sa = Mock(SystemActions)
        sa.isHWInventoryEmpty() >> true
        underTest_.actOnData(data, Mock(NetworkListenerConfig), sa)
        expect: true
    }

    // foreignBs is guaranteed not to be null by the scn translator
    def "toBootState - expected foreign component state"() {
        expect: underTest_.toBootState(foreignBs) == bs

        where:
        foreignBs   || bs
        "Active"       || null
        "On"        || BootState.NODE_ONLINE
        "Off"       || BootState.NODE_OFFLINE
    }

    def "toBootState - unexpected foreign component state"() {
        when: underTest_.toBootState(foreignBs)
        then: thrown exception

        where:
        foreignBs   || exception
        ""          || NetworkListenerProviderException
        "Cow"       || NetworkListenerProviderException
    }

    def "now"() {
        expect: underTest_.currentUTCInNanoseconds() > 0
    }
}
