package com.intel.dai.dsimpl.jdbc

import spock.lang.Specification
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import com.intel.dai.exceptions.DataStoreException

class DbConnectionFactorySpec extends Specification {
    def "DbConnectionFactory constructor"() {
        expect: new DbConnectionFactory() != null
    }
//    def "createDefaultConnection"() {
//        when: DbConnectionFactory.createDefaultConnection()
//        then: thrown Exception
//    }
//    def "ConnectionCreator.createConnection url"() {
//        when: DbConnectionFactory.connCreator.createConnection(null)
//        then: thrown Exception
//    }
//    def "ConnectionCreator.createConnection url, username, password"() {
//        when: DbConnectionFactory.connCreator.createConnection(null, "username", "password")
//        then: thrown Exception
//    }
}

class JdbcStoreTelemetrySpec extends Specification {
    def "createConnection"() {
        def ts = new JdbcStoreTelemetry()
        ts.connection_ = Mock(Connection)
        when: ts.createConnection()
        then: notThrown Exception
    }
    def "createlogEnvDataAggregatedPreparedCall"() {
        def ts = new JdbcStoreTelemetry()
        ts.telemetryAggregatedData_ = Mock(PreparedStatement)
        when: ts.createlogEnvDataAggregatedPreparedCall()
        then: notThrown Exception
    }
}
