package com.intel.dai.dsimpl.jdbc

import com.intel.dai.exceptions.DataStoreException
import com.intel.logging.Logger
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
        when: ts.getCharacteristicsOfLastRawDimmIngested()
        then: true
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