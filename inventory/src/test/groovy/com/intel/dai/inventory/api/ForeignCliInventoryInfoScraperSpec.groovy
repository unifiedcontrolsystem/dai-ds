// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api

import com.intel.dai.inventory.api.pojo.scraped.Bios
import com.intel.dai.inventory.api.pojo.scraped.Dimm
import com.intel.dai.inventory.api.pojo.scraped.DimmBank
import com.intel.dai.inventory.api.pojo.scraped.Fru
import com.intel.logging.Logger
import org.apache.commons.lang3.tuple.ImmutablePair
import spock.lang.Specification

class ForeignCliInventoryInfoScraperSpec extends Specification {
    ForeignCliInventoryInfoScraper ts

    def setup() {
        ts = new ForeignCliInventoryInfoScraper(Mock(Logger))
    }

    def "Trivial test - DimmBank"() {
        DimmBank dimmBank = new DimmBank()
        expect:
        dimmBank.toString() == dimmBank.toString()
    }

    def "Trivial test - Dimm"() {
        Dimm dimm = new Dimm()
        expect:
        dimm.toString() == dimm.toString()
    }

    def "parseDimmEntry - negative"() {
        expect:
        ts.parseDimmEntry(input) == result

        where:
        input                                                                     || result
        ""                                                                        || null
        "CPU2_DIMM_B1 Size 16384 MB Rank 2 Serial 7314D144 Part HMA82GR7CJR8N-WM" || null
    }

    def "parseDimmEntry - positive"() {
        def input = "CPU2_DIMM_B1 Size 16384 MB Rank 2 Serial 7314D144 Part HMA82GR7CJR8N-WM  2933 MT/s"
        def result = new Dimm()
        result.Location = "CPU2_DIMM_B1"
        result.Size = "16384 MB"
        result.Rank = 2
        result.Serial = "7314D144"
        result.Part = "HMA82GR7CJR8N-WM"
        result.Speed = "2933 MT/s"

        expect:
        ts.parseDimmEntry(input) == result
    }

    def "dimmHostLine - negative"() {
        expect:
        ts.parseDimmHostLine(input) == result

        where:
        input || result
        ""    || null
        "dai" || null
    }

    def "dimmHostLine - positive"() {
        def input = "leader1:"
        def result = "leader1"

        expect:
        ts.parseDimmHostLine(input) == result
    }

    def "parseFruHostLine - negative"() {
        expect:
        ts.parseFruHostLine(input) == result

        where:
        input          || result
        ""             || null
        "x3002c0r22b0" || null
    }

    def "parseFruHostLine - positive"() {
        def input = "Host: leader1 Serial: BQWL94103669"
        def result = new Fru()
        result.Host = "leader1"
        result.Serial = "BQWL94103669"

        expect:
        ts.parseFruHostLine(input) == result
    }

    def "parseBiosLine - negative"() {
        expect:
        ts.parseBiosLine(input) == result

        where:
        input               || result
        ""                  || null
        "BIOS Revision 0.0" || null
    }

    def "parseBiosLine - positive"() {
        def input = "BIOS : BIOS Revision 0.0  Firmware Revision 0.0  Version SE5C620.86B.02.01.0010.C0001.010620200716  ROM Size 4096 kB  Address 0xF0000  Release Date 01/06/2020"
        def result = new Bios()
        result.BIOS_Revision = "0.0"
        result.Firmware_Revision = "0.0"
        result.Version = "SE5C620.86B.02.01.0010.C0001.010620200716"
        result.ROM_Size = "4096 kB"
        result.Address = "0xF0000"
        result.Release_Date = "01/06/2020"
        expect:
        ts.parseBiosLine(input) == result
    }

    def "parseGeneralLine - negative"() {
        expect:
        ts.parseGeneralLine(input) == result

        where:
        input               || result
        ""                  || null
        "BIOS Revision 0.0" || null
    }

    def "parseGeneralLine - positive"() {
        def input = "Board Product : A2UX8X4RISER"
        def result = new ImmutablePair<String, String>("Board Product", "A2UX8X4RISER")

        expect:
        ts.parseGeneralLine(input) == result
    }

    def "parseFruGeneralLine - negative"() {
        def fru = new Fru()
        expect:
        ts.parseFruGeneralLine(input, fru) == fru

        where:
        input                          | notNecessary
        ""                             | null
        "Board Produkt : A2UX8X4RISER" | null
    }

    def "parseFruGeneralLine - positive"() {
        def input = "Board Product : A2UX8X4RISER"
        def fru = new Fru()

        expect:
        ts.parseFruGeneralLine(input, fru) == fru
        fru.Board_Product == 'A2UX8X4RISER'
    }
}
