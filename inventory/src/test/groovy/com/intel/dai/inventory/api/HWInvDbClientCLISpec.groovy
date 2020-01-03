package com.intel.dai.inventory.api

import com.intel.dai.dsimpl.voltdb.VoltDbClient
import org.voltdb.client.Client

class HWInvDbClientCLISpec extends spock.lang.Specification {
    HWInvDbClientCLI cli

    def setupSpec() {
        VoltDbClient.voltClient = Mock(Client)
    }

    def setup() {
        cli = new HWInvDbClientCLI()
    }

    // Nonexistent canonical file no longer cause failure
    // Nonexistent vendor file still cause failure because of translation failure
    // Failure to connect no longer cause failure; only stack trace is printed.
    // Hence, UCS voltdb connection() cannot fail.
    def "Test run args - negative" () {
        String[] myArgs = args
        expect: cli.run(myArgs) == expectedValue
        where:
        args                                                                                              || expectedValue
        []                                                                                                || 1
        ["-m", "noSuchMode", "-i", "doesNotMatter"]                                                       || 1
        ["-m", "v2d", "-i", "noSuchFile"]                                                                 || 1
        ["-m", "v2d", "-i", "doesNotMatter", "-s", "invalidURL"]                                          || 1
        ["-m", "c2d", "-i", "noSuchFile"]                                                                 || 1
        ["-m", "c2d", "-i", "doesNotMatter", "-s", "invalidURL"]                                          || 1
    }
    def "Test run args - v2d - negative" () {
        String[] myArgs = ["-m", "v2d", "-i", "src/test/resources/data/HWInvTreeOneNode.sql"]
        expect: cli.run(myArgs) == 1
    }
}
