// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intel.dai.dsapi.HWInvHistory;
import com.intel.dai.dsapi.HWInvHistoryEvent;
import com.intel.dai.dsapi.HWInvLoc;
import com.intel.dai.dsapi.HWInvTree;
import com.intel.dai.dsapi.HWInvUtil;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.dai.foreign_bus.*;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Performs translation of foreign json data to canonical representation.
 */
public class HWInvTranslator {
    private static final Logger logger = LoggerFactory.getInstance("CLIApi", "HWInvTranslator", "console");

    private static final String emptyPrefix = "empty-";

    private final transient Gson gson;
    private final transient HWInvUtil util;

    /**
     * <p> Constructor for the HW inventory translator. </p>
     * @param util utility library containing file I/O code and code to manipulate HW inventory in canonical form
     */
    public HWInvTranslator(HWInvUtil util) {
        this.util = util;

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();
    }

    /**
     * <p> Parses a node JSON format.
     * The json can be in one of two different formats: FullyFlat or Hierarchical.
     * This hierarchical parser can handle the other format.  So, only one parser is necessary. </p>
     *
     * @param foreignJson string containing a node in foreign format
     * @return POJO containing the parsed json, or null if parsing failed
     */
    private ForeignHWInvByLocNode toForeignHWInvByLocNode(String foreignJson) {
        try {
            return gson.fromJson(foreignJson, ForeignHWInvByLocNode.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            return null;
        }
    }

    // Commenting out code for next milestone for now.
    /**
     * <p> Parse the given json containing the foreign HW inventory history. </p>
     * @param foreignHWInvHistoryJson string containing the foreign server response to a HW inventory history query
     * @return POJO containing the parsed json, or null if parsing failed
     */
/*    private ForeignHWInvHistory toForeignHWInvHistory(String foreignHWInvHistoryJson) {
        try {
            return gson.fromJson(foreignHWInvHistoryJson, ForeignHWInvHistory.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            return null;
        }
    }*/

    /**
     * <p> Parses an array of FullyFlat ForeignHWInvByLocNodes.
     * The defining feature is the json is an array. </p>
     *
     * @return a list of POJOs each encoding a HW inventory location
     */
    private ForeignHWInvByLoc[] toForeignHWInvByLocList(String foreignJson) {
        try {
            return gson.fromJson(foreignJson, ForeignHWInvByLoc[].class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            return null;
        }
    }

    /**
     * <p> Parses a structure of named lists of ForeignHWInvByLocNodes.
     * Explicit Format: FullyFlat, Hierarchical or NestNodesOnly
     * The defining attribute is "Format". </p>
     *
     * @param foreignJson string containing a json containing a node in foreign format
     * @return POJO containing the parsed foreign inventory
     */
    private ForeignHWInventory toForeignHWInventory(String foreignJson) {
        try {
            return gson.fromJson(foreignJson, ForeignHWInventory.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            return null;
        }
    }

    /**
     * <p> This method performs JSON to JSON translation of a Foreign HW Inventory tree to its
     * canonical representation. The translated file is written the the file system.  Failure is
     * indicated by a null return value. </p>
     * @return subject of the conversion
     */
    ImmutablePair<String, String> foreignToCanonical(Path inputFile) {
        try {
            return foreignToCanonical(util.fromFile(inputFile));
        } catch (IOException e) {
            logger.error("Failed to read %s: %s", inputFile.toString(), e.getMessage());
            return new ImmutablePair<>(null, null);
        }
    }

    /**
     * <p> Convert a json string containing a HW inventory in foreign format to its
     * canonical representation. Note that if the foreign json is a list, the
     * HW inventory may not be a tree. </p>
     *
     * @param foreignJson json string containing a HW inventory in foreign format
     * @return a pair of the subject of the HW inventory and the json string
     * containing the HW inventory in canonical form
     */
    public ImmutablePair<String, String> foreignToCanonical(String foreignJson) {
        ImmutablePair<String, HWInvTree> translatedResult = toCanonical(foreignJson);
        String subject = translatedResult.getKey();
        ArrayList<HWInvLoc> translatedLocs = new ArrayList<>();
        HWInvTree canonicalTree = translatedResult.getValue();
        if (canonicalTree == null) {
            return ImmutablePair.nullPair();
        }
        for (HWInvLoc loc: canonicalTree.locs) {
            try {
                loc.ID = CommonFunctions.convertForeignToLocation(loc.ID);
                translatedLocs.add(loc);
            } catch(ConversionException e) {
                logger.error("Failed to read JSON from stream: %s", e.getMessage());
                // drop this entry
            }
        }

        canonicalTree.locs = translatedLocs;
        return new ImmutablePair<>(subject, util.toCanonicalJson(canonicalTree));
    }

    // Commenting out code for next milestone for now.
/*    public ImmutablePair<String, String> foreignHistoryToCanonical(String foreignJson) {
        ImmutablePair<String, HWInvHistory> translatedResult = toCanonicalHistory(foreignJson);
        String subject = translatedResult.getKey();
        ArrayList<HWInvHistoryEvent> translatedEvents = new ArrayList<>();
        HWInvHistory canonicalHistory = translatedResult.getValue();
        if (canonicalHistory == null) {
            return ImmutablePair.nullPair();
        }
        for (HWInvHistoryEvent evt: canonicalHistory.events) {
            try {
                evt.ID = CommonFunctions.convertForeignToLocation(evt.ID);
                translatedEvents.add(evt);
            } catch(ConversionException e) {
                logger.error("Failed to read JSON from stream: %s", e.getMessage());
                // drop this entry
            }
        }

        canonicalHistory.events = translatedEvents;
        return new ImmutablePair<>(subject, util.toCanonicalHistoryJson(canonicalHistory));
    }*/

    // Commenting out code for next milestone for now.
/*    private ImmutablePair<String, HWInvHistory> toCanonicalHistory(String foreignJson) {
        ForeignHWInvHistory hist = toForeignHWInvHistory(foreignJson);
        if (hist != null) {
            logger.info("Parsed toForeignHWInvHistory");
            return new ImmutablePair<>("", toCanonical(hist));
        }
        return ImmutablePair.nullPair();
    }*/

    /**
     * <p> Make several attempts at translating the location described by the input file.  Return upon the first
     * success.
     * </p>
     * @param foreignJson string contain a json of a HW inventory locations in foreign format
     * @return pair containing subject of the input HW inventory json and its content as a HWInvTree object
     */
    private ImmutablePair<String, HWInvTree> toCanonical(String foreignJson) {
        logger.info("Attempt toForeignHWInvByLocList");
        ForeignHWInvByLoc[] frus = toForeignHWInvByLocList(foreignJson);
        if (frus != null) {
            logger.info("Parsed toForeignHWInvByLocList");
            return new ImmutablePair<>("", toCanonical(frus));      // location list has no explicit subject
        }

        logger.info("Attempt toForeignHWInventory");
        ForeignHWInventory foreignTree = toForeignHWInventory(foreignJson);
        if (foreignTree != null) {
            logger.info("Parsed toForeignHWInventory");
            if (foreignTree.ForeignName != null) {
                if (foreignTree.Format == null) {
                    logger.error("Missing format field");
                    return ImmutablePair.nullPair();
                }
                logger.info("Parsed HW Inventory");
                String root = foreignTree.ForeignName;
                if (!isValidLocationName(root)) {
                    logger.error("ForeignHWInventory must have valid subject");
                    return ImmutablePair.nullPair();
                }
                return new ImmutablePair<>(root, toCanonical(foreignTree));
            }
            logger.info("ForeignName is null");
        }

        logger.info("Attempt toForeignHWInvByLocNode");
        ForeignHWInvByLocNode node = toForeignHWInvByLocNode(foreignJson);
        if (node != null) {
            logger.info("Parsed toForeignHWInvByLocNode");
            if (!node.Type.equals("Node")) {  // only support node for now
                logger.error("Unsupported hw Loc type: %s", node.Type);
                return ImmutablePair.nullPair();
            }
            logger.info("Parsed HW Loc Node");
            String root = node.ID;
            if (!isValidLocationName(root)) {
                logger.error("ForeignHWInventory must have valid subject");
                return ImmutablePair.nullPair();
            }
            return new ImmutablePair<>(root, toCanonical(node));
        }

        return ImmutablePair.nullPair();
    }

    /**
     * <p> I decided not to implement this using reflection for now.  Using reflection may
     * make the parser too general.  However, it may reduce the number of lines of code. </p>
     *
     * @param node a POJO containing a node location in foreign format
     * @return a canonical HW inventory tree containing the input node
     */
    private HWInvTree toCanonical(ForeignHWInvByLocNode node) {
        HWInvTree canonicalTree = new HWInvTree();
        ArrayList<ForeignHWInvByLocNode> singletonNode = new ArrayList<>();
        singletonNode.add(node);
        addForeignNodesToCanonical(singletonNode, canonicalTree);
        return canonicalTree;
    }

    private HWInvTree toCanonical(ForeignHWInvByLoc[] frus) {
        HWInvTree canonicalTree = new HWInvTree();
        addForeignFlatFRUsToCanonical(
                Arrays.stream(frus).collect(Collectors.toList()),
                canonicalTree);
        return canonicalTree;
    }

    private HWInvHistory toCanonical(ForeignHWInvHistory foreignHist) {
        HWInvHistory hist = new HWInvHistory();
        for (ForeignHWInvHistoryAtLoc componentHistory: foreignHist.Components) {
            for (ForeignHWInvHistoryEvent foreignEvent : componentHistory.History) {
                HWInvHistoryEvent event = toCanonical(foreignEvent);
                if (event == null) {
                    continue;   //ignore failed conversions
                }
                logger.info("Adding %s", event.toString());
                hist.events.add(event);
            }
        }
        return hist;
    }

    /**
     * <p> This method performs POJO to POJO translation of a Foreign HW Inventory tree to its
     * canonical representation.
     * </p>
     * @param foreignTree the Foreign POJO containing the HW inventory
     * @return the canonical POJO containing the HW inventory
     * @since 1.0.0
     */
    private HWInvTree toCanonical(ForeignHWInventory foreignTree) {
        HWInvTree canonicalTree = new HWInvTree();
        int numLocAdded = 0;

        numLocAdded += addForeignCabinetsCanonical(foreignTree.Cabinets, canonicalTree);
        numLocAdded += addForeignChassisToCanonical(foreignTree.Chassis, canonicalTree);
        numLocAdded += addForeignComputeModulesToCanonical(foreignTree.ComputeModules, canonicalTree);
        numLocAdded += addForeignRouterModulesToCanonical(foreignTree.RouterModules, canonicalTree);

        numLocAdded += addForeignNodeEnclosuresToCanonical(foreignTree.NodeEnclosures, canonicalTree);
        numLocAdded += addForeignHSNBoardsToCanonical(foreignTree.HSNBoards, canonicalTree);
        numLocAdded += addForeignNodesToCanonical(foreignTree.Nodes, canonicalTree);
        numLocAdded += addForeignProcessorsToCanonical(foreignTree.Processors, canonicalTree);

        numLocAdded += addForeignMemoryToCanonical(foreignTree.Memory, canonicalTree);
        numLocAdded += addForeignCabinetPDUToCanonical(foreignTree.CabinetPDU, canonicalTree);
        numLocAdded += addForeignCabinetPDUOutletsToCanonical(foreignTree.CabinetPDUOutlets, canonicalTree);

        logger.info("num HW locations added = %d", numLocAdded);
        return canonicalTree;
    }

    private int addForeignCabinetPDUOutletsToCanonical(List<ForeignHWInvByLocCabinetPDUOutlet> cabinetPDUOutlets,
                                                       HWInvTree canonicalTree) {
        return addForeignFRUsNotChildrenToCanonical(cabinetPDUOutlets, canonicalTree);
    }

    private int addForeignMemoryToCanonical(List<ForeignHWInvByLocMemory> memory, HWInvTree canonicalTree) {
        return addForeignFRUsNotChildrenToCanonical(memory, canonicalTree);
    }

    private int addForeignProcessorsToCanonical(List<ForeignHWInvByLocProcessor> processors, HWInvTree canonicalTree) {
        return addForeignFRUsNotChildrenToCanonical(processors, canonicalTree);
    }

    private int addForeignHSNBoardsToCanonical(List<ForeignHWInvByLocHSNBoard> hsnBoards, HWInvTree canonicalTree) {
        return addForeignFRUsNotChildrenToCanonical(hsnBoards, canonicalTree);
    }

    private int addForeignRouterModulesToCanonical(List<ForeignHWInvByLocRouterModule> routerModules,
                                                   HWInvTree canonicalTree) {
        return addForeignFRUsNotChildrenToCanonical(routerModules, canonicalTree);
    }

    private int addForeignCabinetsCanonical(List<ForeignHWInvByLocCabinet> foreignCabinets,
                                            HWInvTree canonicalTree) {
        int numAdded = addForeignFRUsNotChildrenToCanonical(foreignCabinets, canonicalTree);
        if (numAdded == 0) {
            return 0;
        }

        for (ForeignHWInvByLocCabinet cab : foreignCabinets) {
            numAdded += addForeignChassisToCanonical(cab.Chassis, canonicalTree);
        }
        return numAdded;
    }

    private int addForeignChassisToCanonical(List<ForeignHWInvByLocChassis> foreignChassis,
                                             HWInvTree canonicalTree) {
        int numAdded = addForeignFRUsNotChildrenToCanonical(foreignChassis, canonicalTree);
        if (numAdded == 0) {
            return 0;
        }

        for (ForeignHWInvByLocChassis ch : foreignChassis) {
            numAdded += addForeignComputeModulesToCanonical(ch.ComputeModules, canonicalTree);
        }
        return numAdded;
    }

    private int addForeignComputeModulesToCanonical(List<ForeignHWInvByLocComputeModule> foreignComputeModules,
                                                    HWInvTree canonicalTree) {
        int numAdded = addForeignFRUsNotChildrenToCanonical(foreignComputeModules, canonicalTree);
        if (numAdded == 0) {
            return 0;
        }

        for (ForeignHWInvByLocComputeModule cm : foreignComputeModules) {
            numAdded += addForeignNodeEnclosuresToCanonical(cm.NodeEnclosures, canonicalTree);
        }
        return numAdded;
    }

    private int addForeignNodeEnclosuresToCanonical(List<ForeignHWInvByLocNodeEnclosure> nodeEnclosures,
                                                    HWInvTree canonicalTree) {
        int numAdded = addForeignFRUsNotChildrenToCanonical(nodeEnclosures, canonicalTree);
        if (numAdded == 0) {
            return 0;
        }

        for (ForeignHWInvByLocNodeEnclosure ne : nodeEnclosures) {
            numAdded += addForeignNodesToCanonical(ne.Nodes, canonicalTree);
        }
        return numAdded;
    }

    private int addForeignNodesToCanonical(List<ForeignHWInvByLocNode> foreignNodeList,
                                           HWInvTree canonicalTree) {
        int numAdded = addForeignFRUsNotChildrenToCanonical(foreignNodeList, canonicalTree);
        if (numAdded == 0) {
            return 0;
        }

        for (ForeignHWInvByLocNode node : foreignNodeList) {
            numAdded += addForeignFRUsNotChildrenToCanonical(node.Processors, canonicalTree);
            numAdded += addForeignFRUsNotChildrenToCanonical(node.Memory, canonicalTree);
        }
        return numAdded;
    }
    private int addForeignCabinetPDUToCanonical(List<ForeignHWInvByLocCabinetPDU> foreigncabinetPduList,
                                                HWInvTree canonicalTree) {
        int numAdded = addForeignFRUsNotChildrenToCanonical(foreigncabinetPduList, canonicalTree);
        if (numAdded == 0) {
            return 0;
        }

        for (ForeignHWInvByLocCabinetPDU cabPdu : foreigncabinetPduList) {
            numAdded += addForeignFRUsNotChildrenToCanonical(cabPdu.CabinetPDUOutlets, canonicalTree);
        }
        return numAdded;
    }

    /**
     * <p> Add only the FRU denoted by the vertex of the specified canonical tree. </p>
     *
     * @param foreignFRUList list of FRUs to be added to the canonical tree
     * @param canonicalTree POJO representing the HW inventory in canonical form
     * @return number of entries added to the DB.
     */
    private <T> int addForeignFRUsNotChildrenToCanonical(List<T> foreignFRUList,
                                                         HWInvTree canonicalTree) {
        List<ForeignHWInvByLoc> flatFrus = downcastToForeignHWInvByLocList(foreignFRUList);
        return addForeignFlatFRUsToCanonical(flatFrus, canonicalTree);
    }

    private <T> List<ForeignHWInvByLoc> downcastToForeignHWInvByLocList(List<T> foreignFRUList) {
        if (foreignFRUList == null) {
            return null;
        }

        List<ForeignHWInvByLoc> flatFrus = new ArrayList<>();
        for (T fru : foreignFRUList) {
            flatFrus.add((ForeignHWInvByLoc) fru);  // downcast to flat fru
        }
        return flatFrus;
    }

    private int addForeignFlatFRUsToCanonical(List<ForeignHWInvByLoc> foreignLocList,
                                              HWInvTree canonicalTree) {
        if (foreignLocList == null) {
            return 0;
        }

        int numSlotsAdded = 0;
        for (ForeignHWInvByLoc loc: foreignLocList) {
            HWInvLoc slot = toCanonical(loc);
            if (slot == null) {
                continue;   //ignore failed conversions
            }
            logger.info("Adding %s", slot.toString());
            canonicalTree.locs.add(slot);
            numSlotsAdded++;
        }
        return numSlotsAdded;
    }

    HWInvHistoryEvent toCanonical(ForeignHWInvHistoryEvent foreignEvent) {
        logger.info("foreignEvent: %s", foreignEvent.toString());
        if (foreignEvent.ID == null) {
            return null;
        }
        if (foreignEvent.FRUID == null) {
            return null;
        }
        if (foreignEvent.Timestamp == null) {
            return null;
        }
        if (foreignEvent.EventType == null) {
            return null;
        }
        HWInvHistoryEvent event = new HWInvHistoryEvent();

        event.ID = foreignEvent.ID;
        event.Action = foreignEvent.EventType;
        event.Timestamp = foreignEvent.Timestamp;
        event.FRUID = foreignEvent.FRUID;

        return event;
    }

    HWInvLoc toCanonical(ForeignHWInvByLoc foreignLoc) {
        logger.info("foreignLoc: %s", foreignLoc.toString());
        if (foreignLoc.ID == null) {
            return null;
        }
        if (foreignLoc.Type == null) {
            return null;
        }
        if (foreignLoc.Ordinal < 0) {
            return null;
        }
        if (foreignLoc.Status == null) {
            return null;
        }
        HWInvLoc canonicalFru = new HWInvLoc();

        canonicalFru.ID = foreignLoc.ID;    // + nodeTypeAbbreviation.get(foreignLoc.Type) + foreignLoc.Ordinal;
        canonicalFru.Type = foreignLoc.Type;
        canonicalFru.Ordinal = foreignLoc.Ordinal;

        switch (foreignLoc.Status) {
            case "Empty":
                if (foreignLoc.PopulatedFRU != null) {
                    logger.error("Empty slot should have null PopulatedFRU");
                    return null;    // best not put cooked data into the DB
                }
                canonicalFru.FRUID = emptyPrefix + canonicalFru.ID; // unique ID to satisfy SQL uniqueness constraint
                return canonicalFru;
            case "Populated":
                if (foreignLoc.PopulatedFRU == null) {
                    logger.error("PopulatedFRU should not be null");
                    return null;    // best not put cooked bad data into the DB
                }
                canonicalFru.FRUID = foreignLoc.PopulatedFRU.FRUID;
                canonicalFru.FRUType = foreignLoc.PopulatedFRU.Type;
                canonicalFru.FRUSubType = foreignLoc.PopulatedFRU.Subtype;
                return canonicalFru;
            default:
                logger.error("Unknown status: %s", foreignLoc.Status);
                return null;
        }
    }

    String extractParentId(String id) {
        try {
            Pattern pattern = Pattern.compile("(^.*)\\D\\d+$");
            Matcher matcher = pattern.matcher(id);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return "";
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Detemine if this is a valid location name.  An example of a valid
     * location name looks like a0b34c89.  Specially, null and "" are not
     * valid location names.
     * @param location HW inventory location
     * @return true iff the given location is valid
     */
    boolean isValidLocationName(String location) {
        if (location == null) {
            return false;
        }
        Pattern pattern = Pattern.compile("^([a-z]\\d+)+$");
        Matcher matcher = pattern.matcher(location);
        return matcher.find();
    }
}
