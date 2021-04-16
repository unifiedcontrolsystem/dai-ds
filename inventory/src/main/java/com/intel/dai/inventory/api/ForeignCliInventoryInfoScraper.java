// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api;

import com.intel.dai.inventory.api.pojo.scraped.Bios;
import com.intel.dai.inventory.api.pojo.scraped.Dimm;
import com.intel.dai.inventory.api.pojo.scraped.Fru;
import com.intel.logging.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * We assume that the raw data is in large text file.  We have 2 voltdb tables: FRU and DIMM.
 * Each FRU row contains a FRU.  Each DIMM row contains a DIMM in a host FRU.  These 2 tables
 * are cooked into node jsons, which are then inserted into the node inventory history.
 */
public class ForeignCliInventoryInfoScraper {
    private final Logger logger;

    public ForeignCliInventoryInfoScraper(Logger log) {
        logger = log;
    }

    public Dimm parseDimmEntry(String input) {
        String patString = "^(\\S+)\\s+"
                + "Size\\s+(\\d+\\s+\\S+)\\s+"
                + "Rank\\s+(\\d+)\\s+"
                + "Serial\\s+(\\S+)\\s+"
                + "Part\\s+(\\S+)\\s+"
                + "(\\d+\\s+\\S+)\\s*"
                + "$";
        Matcher matcher = parse(patString, input);
        if (matcher.find()) {
            Dimm dimm = new Dimm();
            dimm.Location = matcher.group(1).trim();
            dimm.Size = matcher.group(2).trim();
            dimm.Rank = Integer.parseInt(matcher.group(3).trim());
            dimm.Serial = matcher.group(4).trim();
            dimm.Part = matcher.group(5).trim();
            dimm.Speed = matcher.group(6).trim();
            return dimm;
        }
        logger.error("Cannot parse dimm entry");
        return null;
    }

    public String parseDimmHostLine(String input) {
        Matcher matcher = parse("^(\\S+):\\s*$", input);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    public Fru parseFruHostLine(String input) {
        Matcher matcher = parse("^Host:\\s*(\\S+)\\s+Serial:\\s*(\\S+)\\s*$", input);
        if (matcher.find()) {
            Fru fru = new Fru();
            fru.Host = matcher.group(1).trim();
            fru.Serial = matcher.group(2).trim();
            return fru;
        }
        return null;
    }


    /**
     * Client code should trim the input of whitespace.
     *
     * @param input line from a fru file
     * @return Bios POJO
     */
    public Bios parseBiosLine(String input) {
        String patString = "^BIOS\\s*:\\s*BIOS Revision\\s+(\\S+)\\s+Firmware Revision\\s+(\\S+)\\s+" +
                "Version\\s+(\\S+)\\s+ROM Size\\s+(\\d+ \\S+)\\s+Address\\s+(\\S+)\\s+Release Date\\s+(\\S+)\\s*$";
        Matcher matcher = parse(patString, input);
        if (matcher.find()) {
            Bios bios = new Bios();
            bios.BIOS_Revision = matcher.group(1).trim();
            bios.Firmware_Revision = matcher.group(2).trim();
            bios.Version = matcher.group(3).trim();
            bios.ROM_Size = matcher.group(4).trim();
            bios.Address = matcher.group(5).trim();
            bios.Release_Date = matcher.group(6).trim();
            return bios;
        }
        return null;
    }

    public Fru parseFruGeneralLine(String input, Fru fru) {
        ImmutablePair<String, String> pair = parseGeneralLine(input);
        if (pair == null) return fru;

        switch (pair.left) {
            case "Board Serial":
                fru.Board_Serial = pair.right;
                break;
            case "Chassis Extra":
                fru.Chassis_Extra = pair.right;
                break;
            case "Product Part Number":
                fru.Product_Part_Number = pair.right;
                break;
            case "Product Serial":
                fru.Product_Serial = pair.right;
                break;
            case "Chassis Serial":
                fru.Chassis_Serial = pair.right;
                break;
            case "Board Part Number":
                fru.Board_Part_Number = pair.right;
                break;
            case "Product Manufacturer":
                fru.Product_Manufacturer = pair.right;
                break;
            case "Chassis Type":
                fru.Chassis_Type = pair.right;
                break;
            case "Product Asset Tag":
                fru.Product_Asset_Tag = pair.right;
                break;
            case "Chassis Part Number":
                fru.Chassis_Part_Number = pair.right;
                break;
            case "Board Product":
                fru.Board_Product = pair.right;
                break;
            case "Board Extra":
                fru.Board_Extra = pair.right;
                break;
            case "Product Version":
                fru.Product_Version = pair.right;
                break;
            case "Board Mfg":
                fru.Board_Mfg = pair.right;
                break;
            case "Board Mfg Date":
                fru.Board_Mfg_Date = pair.right;
                break;
            case "BMC Firmware Revision":
                fru.BMC_Firmware_Revision = pair.right;
                break;
            case "BMC Auxiliary Firmware Revision Information":
                fru.BMC_Auxiliary_Firmware_Revision_Information = pair.right;
                break;
            default:
                logger.error("Unexpected key: %s", pair.left);
        }
        return fru;
    }

    ImmutablePair<String, String> parseGeneralLine(String input) {
        Matcher matcher = parse("^(.+\\b)\\s*:\\s*(.+)\\s*$", input);
        if (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            return new ImmutablePair<>(key, value);
        }
        return null;
    }

    Matcher parse(String patString, String input) {
        Pattern pattern = Pattern.compile(patString);
        return pattern.matcher(input);
    }
}
