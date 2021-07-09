package com.intel.dai.dsimpl.jdbc

import com.intel.dai.exceptions.DataStoreException
import com.intel.logging.Logger
import org.apache.commons.lang3.tuple.ImmutablePair
import spock.lang.Specification

import java.sql.Connection
import java.sql.DriverManager

class InventorySnapshotJdbcITSpec extends Specification {
    Logger logger = Mock Logger
    DbConnectionFactory factory = Mock DbConnectionFactory

    InventorySnapshotJdbc ts = new InventorySnapshotJdbc(logger)

    def setup() {
        ts.dbConn = postgresConn()
    }

    def postgresConn() {
        return DriverManager
                .getConnection("jdbc:postgresql://css-centos-8-00.ra.intel.com:5432/dai",
                        "postgres", "postgres")
    }

    def "trivial"() {
        expect: ts != null
    }

//    def "establishConnectionToNearlineDb" () {
//        when: ts.establishConnectionToNearlineDb()
//        then: thrown DataStoreException
//    }

    def "getCharacteristicsOfLastRawDimmIngested"() {
        when:
        ImmutablePair<Long, String> res = ts.getCharacteristicsOfLastRawDimmIngested()
        then:
        res.left == 1000002
        res.right == 'p_serial2'
    }

    def "isRawDimmDuplicated -- true"() {
        expect: ts.isRawDimmDuplicated(serial, timestamp) == result

        where:
        serial      | timestamp || result
        'p_serial2' | 1000002   || true
        'p_serial2' | 1000001   || false
    }

    def "isRawFruHostDuplicated"() {
        expect: ts.isRawFruHostDuplicated(mac, timestamp) == result

        where:
        mac      | timestamp || result
        'p_mac2' | 1000005   || true
        'p_mac2' | 1000003   || false
    }

    def "getCharacteristicsOfLastRawFruHostIngested"() {
        when:
        ImmutablePair<Long, String> res = ts.getCharacteristicsOfLastRawFruHostIngested()
        then:
        res.left == 1000005
        res.right == 'p_mac2'
    }
}

//private void establishConnectionToNearlineDb() throws DataStoreException {
//    if (dbConn == null) {
//        dbConn = DbConnectionFactory.createDefaultConnection();
//        if (dbConn == null) {
//            throw new DataStoreException("DbConnectionFactory.createDefaultConnection() => null");
//        }
//    }
//}