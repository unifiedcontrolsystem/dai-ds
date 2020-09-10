// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.intel.dai.dsapi.*;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.dai.inventory.api.pojo.hist.ForeignHWInvHistory;
import com.intel.dai.inventory.api.pojo.hist.ForeignHWInvHistoryAtLoc;
import com.intel.dai.inventory.api.pojo.hist.ForeignHWInvHistoryEvent;
import com.intel.dai.inventory.api.pojo.loc.*;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The class translates foreign inventory jsons into canonical jsons.
 *
 * There are two categories of canonical inventory jsons: snapshot and history.  The canonical forms are fed to
 * db clients so that they can be ingested into the relevant database tables.
 */
public class HWInvTranslator {
    /**
     * <p> Constructs the HWInvTranslator object by initializing the GSON object, and the util object. </p>
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
     * @return POJO containing the Parsed as json, or null if parsing failed
     */
    private ForeignHWInvByLocNode toForeignHWInvByLocNode(String foreignJson) {
        try {
            return gson.fromJson(foreignJson, ForeignHWInvByLocNode.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            logger.warn("GSON parsing error: %s", e.getMessage());
            return null;
        }
    }

    // Commenting out code for next milestone for now.
//   /**
//    * <p> Parse the given json containing the foreign HW inventory history. </p>
//    * @param foreignHWInvHistoryJson string containing the foreign server response to a HW inventory history query
//    * @return POJO containing the Parsed as json, or null if parsing failed
//    */
    private ForeignHWInvHistory toForeignHWInvHistory(String foreignHWInvHistoryJson) {
        try {
            return gson.fromJson(foreignHWInvHistoryJson, ForeignHWInvHistory.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            logger.fatal("GSON parsing error: %s", e.getMessage());
            return null;
        }
    }

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
            logger.debug("HWI:%n  GSON parsing error: %s", e.getMessage());
            return null;
        }
    }

    private ForeignHWInvByLoc toForeignHWInvByLoc(String foreignJson) {
        try {
            return gson.fromJson(foreignJson, ForeignHWInvByLoc.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            logger.warn("GSON parsing error: %s", e.getMessage());
            return null;
        }
    }

    /**
     * <p> Parses a structure of named lists of ForeignHWInvByLocNodes.
     * Explicit Format: FullyFlat, Hierarchical or NestNodesOnly
     * The defining attribute is "Format". </p>
     *
     * @param foreignJson string containing a json containing a node in foreign format
     * @return POJO containing the Parsed as foreign inventory
     */
    private ForeignHWInventory toForeignHWInventory(String foreignJson) {
        try {
            return gson.fromJson(foreignJson, ForeignHWInventory.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            logger.warn("GSON parsing error: %s", e.getMessage());
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
     * <p> Converts a json string containing a HW inventory in foreign format to its
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

        util.setMaxNumberOfNonDebugMessages(1);
        for (HWInvLoc loc: canonicalTree.locs) {
            try {
                loc.ID = CommonFunctions.convertForeignToLocation(loc.ID);
            } catch (ConversionException e) {
                util.logError("HWI:%n  DAI namespace conversion failure: %s", e.getMessage());
            } catch (NullPointerException e) {
                util.logError("HWI:%n  loc=%s: %s", loc, e.getMessage());
                continue;
            }
            translatedLocs.add(loc);    // always all loc so that we can debug the namespace conversion errors
        }

        canonicalTree.locs = translatedLocs;
        return new ImmutablePair<>(subject, util.toCanonicalJson(canonicalTree));
    }


    public ImmutablePair<String, String> foreignHistoryToCanonical(String foreignJson) {
        ImmutablePair<String, HWInvHistory> translatedResult = toCanonicalHistory(foreignJson);
        String subject = translatedResult.getKey();
        ArrayList<HWInvHistoryEvent> translatedEvents = new ArrayList<>();
        HWInvHistory canonicalHistory = translatedResult.getValue();
        if (canonicalHistory == null) {
            return ImmutablePair.nullPair();
        }

        util.setMaxNumberOfNonDebugMessages(1);
        for (HWInvHistoryEvent evt: canonicalHistory.events) {
            try {
                evt.ID = CommonFunctions.convertForeignToLocation(evt.ID);
            } catch(ConversionException e) {
                util.logError("HWI:%n  convertForeignToLocation(evt.ID=%s) threw %s",
                        evt.ID, e.getMessage());
                // skip translation so we can debug this issue
            }
            translatedEvents.add(evt);
        }

        canonicalHistory.events = translatedEvents;
        return new ImmutablePair<>(subject, util.toCanonicalHistoryJson(canonicalHistory));
    }


    private ImmutablePair<String, HWInvHistory> toCanonicalHistory(String foreignJson) {
        ForeignHWInvHistory hist = toForeignHWInvHistory(foreignJson);
        if (hist != null) {
            logger.debug("HWI:%n  toForeignHWInvHistory(foreignJson=%s)%n =>  %s",
                    util.head(foreignJson,120),
                    util.head(hist.toString(), 120));
            return new ImmutablePair<>("", toCanonical(hist));
        }
        return ImmutablePair.nullPair();
    }

    /**
     * <p> Make several attempts at translating the location described by the input file.  Return upon the first
     * success.
     * </p>
     * @param foreignJson string contain a json of a HW inventory locations in foreign format
     * @return pair containing subject of the input HW inventory json and its content as a HWInvTree object
     */
    private ImmutablePair<String, HWInvTree> toCanonical(String foreignJson) {
        logger.debug("Attempt toForeignHWInvByLocList");
        ForeignHWInvByLoc[] frus = toForeignHWInvByLocList(foreignJson);
        if (frus != null) {
            logger.info("HWI:%n  %s", "Parsed as toForeignHWInvByLocList");
            return new ImmutablePair<>("", toCanonical(frus));      // location list has no explicit subject
        }

        logger.debug("Attempt toForeignHWInventory");
        ForeignHWInventory foreignTree = toForeignHWInventory(foreignJson);
        if (foreignTree != null) {
            logger.debug("HWI:%n  Parsed as toForeignHWInventory");
            if (foreignTree.XName != null) {
                if (foreignTree.Format == null) {
                    logger.error("HWI:%n  Missing format field");
                    return ImmutablePair.nullPair();
                }
                logger.info("HWI:%n  %s", "Parsed as HW Inventory");
                return new ImmutablePair<>(foreignTree.XName, toCanonical(foreignTree));
            }
            logger.debug("HWI:%n  XName is null; try next parsing method");
        }

        logger.debug("HWI:%n  Attempt toForeignHWInvByLocNode");
        ForeignHWInvByLocNode nodeCandidate = toForeignHWInvByLocNode(foreignJson);
        if (nodeCandidate != null && nodeCandidate.Type != null) {
            if (nodeCandidate.Type.equals("Node")) {  // only support node for now
                logger.info("HWI:%n  %s", "Parsed as toForeignHWInvByLocNode");
                return new ImmutablePair<>(nodeCandidate.ID, toCanonical(nodeCandidate));
            }
            logger.debug("HWI:%n  Cannot parse as a node; hw Loc type: %s", nodeCandidate.Type);
        }
        else {
            logger.debug("HWI:%n  nodeCandidate == null || nodeCandidate.Type == null");
        }

        logger.debug("HWI:%n  Attempt toForeignHWInvByLoc (no children will be parsed)");
        ForeignHWInvByLoc loc = toForeignHWInvByLoc(foreignJson);
        if (loc != null && loc.ID != null) {
            logger.info("HWI:%n  %s", "Parsed as toForeignHWInvByLoc");
            return new ImmutablePair<>(loc.ID, toCanonical(loc));
        }

        logger.fatal("HWI:%n  All attempts to convert the foreign inventory json failed: %s", foreignJson);
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

    private HWInvTree toCanonical(ForeignHWInvByLoc loc) {
        HWInvTree canonicalTree = new HWInvTree();
        canonicalTree.locs.add(toCanonicalLoc(loc));
        logger.info("HWI:%n  toCanonical(ForeignHWInvByLoc loc=%s) => %s",
                loc.toString(), canonicalTree.toString());
        return canonicalTree;
    }

    private HWInvTree toCanonical(ForeignHWInvByLoc[] frus) {
        HWInvTree canonicalTree = new HWInvTree();
        addForeignFlatLocsToCanonical(
                Arrays.stream(frus).collect(Collectors.toList()),   // converts an array to a list
                canonicalTree);
        return canonicalTree;
    }


    private HWInvHistory toCanonical(ForeignHWInvHistory foreignHist) {
        HWInvHistory hist = new HWInvHistory();
        for (ForeignHWInvHistoryAtLoc componentHistory: foreignHist.Components) {
            for (ForeignHWInvHistoryEvent foreignEvent : componentHistory.History) {
                HWInvHistoryEvent event = toCanonical(foreignEvent);
                if (event == null) {
                    logger.error("HWI:%n  toCanonical(foreignEvent=%s) failed", foreignEvent);
                    continue;   // ignore failed conversions
                }

                logger.debug("Adding %s", event.toString());
                hist.events.add(event);
            }
        }
        logger.debug("HWI:%n  Number of canonical historical records extracted = %d", hist.events.size());
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

        numLocAdded += addForeignDrivesToCanonical(foreignTree.Drives, canonicalTree);
        numLocAdded += addForeignMemoryToCanonical(foreignTree.Memory, canonicalTree);
        numLocAdded += addForeignCabinetPDUToCanonical(foreignTree.CabinetPDU, canonicalTree);
        numLocAdded += addForeignCabinetPDUOutletsToCanonical(foreignTree.CabinetPDUOutlets, canonicalTree);

        logger.debug("HWI:%n  Number of HW location-fru pairs extracted = %d", numLocAdded);
        return canonicalTree;
    }

    private int addForeignCabinetPDUOutletsToCanonical(List<ForeignHWInvByLocCabinetPDUOutlet> cabinetPDUOutlets,
                                                       HWInvTree canonicalTree) {
        return addForeignLocsNotChildrenToCanonical(cabinetPDUOutlets, canonicalTree);
    }

    private int addForeignDrivesToCanonical(List<ForeignHWInvByLocDrive> drives, HWInvTree canonicalTree) {
        return addForeignLocsNotChildrenToCanonical(drives, canonicalTree);
    }

    private int addForeignMemoryToCanonical(List<ForeignHWInvByLocMemory> memory, HWInvTree canonicalTree) {
        return addForeignLocsNotChildrenToCanonical(memory, canonicalTree);
    }

    private int addForeignProcessorsToCanonical(List<ForeignHWInvByLocProcessor> processors, HWInvTree canonicalTree) {
        return addForeignLocsNotChildrenToCanonical(processors, canonicalTree);
    }

    private int addForeignHSNBoardsToCanonical(List<ForeignHWInvByLocHSNBoard> hsnBoards, HWInvTree canonicalTree) {
        return addForeignLocsNotChildrenToCanonical(hsnBoards, canonicalTree);
    }

    private int addForeignRouterModulesToCanonical(List<ForeignHWInvByLocRouterModule> routerModules,
                                                   HWInvTree canonicalTree) {
        return addForeignLocsNotChildrenToCanonical(routerModules, canonicalTree);
    }

    private int addForeignCabinetsCanonical(List<ForeignHWInvByLocCabinet> foreignCabinets,
                                            HWInvTree canonicalTree) {
        int numAdded = addForeignLocsNotChildrenToCanonical(foreignCabinets, canonicalTree);
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
        int numAdded = addForeignLocsNotChildrenToCanonical(foreignChassis, canonicalTree);
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
        int numAdded = addForeignLocsNotChildrenToCanonical(foreignComputeModules, canonicalTree);
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
        int numAdded = addForeignLocsNotChildrenToCanonical(nodeEnclosures, canonicalTree);
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
        int numAdded = addForeignLocsNotChildrenToCanonical(foreignNodeList, canonicalTree);
        if (numAdded == 0) {
            return 0;
        }

        for (ForeignHWInvByLocNode node : foreignNodeList) {
            numAdded += addForeignLocsNotChildrenToCanonical(node.Processors, canonicalTree);
            numAdded += addForeignLocsNotChildrenToCanonical(node.Memory, canonicalTree);
            numAdded += addForeignLocsNotChildrenToCanonical(node.Drives, canonicalTree);
        }
        return numAdded;
    }

    private int addForeignCabinetPDUToCanonical(List<ForeignHWInvByLocCabinetPDU> foreigncabinetPduList,
                                                HWInvTree canonicalTree) {
        int numAdded = addForeignLocsNotChildrenToCanonical(foreigncabinetPduList, canonicalTree);
        if (numAdded == 0) {
            return 0;
        }

        for (ForeignHWInvByLocCabinetPDU cabPdu : foreigncabinetPduList) {
            numAdded += addForeignLocsNotChildrenToCanonical(cabPdu.CabinetPDUOutlets, canonicalTree);
        }
        return numAdded;
    }

    /**
     * <p> Adds only the FRU denoted by the vertex of the specified canonical tree. </p>
     *
     * @param foreignLocList list of FRUs to be added to the canonical tree
     * @param canonicalTree POJO representing the HW inventory in canonical form
     * @return number of entries added to the DB.
     */
    private <T> int addForeignLocsNotChildrenToCanonical(List<T> foreignLocList,
                                                         HWInvTree canonicalTree) {
        return addForeignFlatLocsToCanonical(foreignLocList, canonicalTree);
    }

    private <T> int addForeignFlatLocsToCanonical(List<T> foreignLocList,
                                              HWInvTree canonicalTree) {
        if (foreignLocList == null) {
            return 0;
        }
        int numLocAdded = 0;
        for (T loc: foreignLocList) {
            HWInvLoc slot = toCanonicalLoc((ForeignHWInvByLoc) loc);
            if (slot == null) {
                logger.error("HWI:%n  toCanonicalLoc(loc=%s) failed", loc);
                continue;   // ignore failed conversions
            }
            logger.debug("Adding %s", slot.toString());
            canonicalTree.locs.add(slot);
            numLocAdded++;
        }
        return numLocAdded;
    }

    HWInvHistoryEvent toCanonical(ForeignHWInvHistoryEvent foreignEvent) {
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

    HWInvLoc toCanonicalLoc(ForeignHWInvByLoc foreignLoc) {
        logger.debug("foreignLoc: %s", foreignLoc.toString());
        if (foreignLoc.ID == null) {
            logger.error("HWI:%n  %s", "foreignLoc.ID cannot be null");
            return null;
        }
        if (foreignLoc.Type == null) {
            logger.error("HWI:%n  %s", "foreignLoc.Type cannot be null");
            return null;
        }
        if (foreignLoc.Ordinal < 0) {
            logger.error("HWI:%n  foreignLoc.Ordinal=%d cannot be negative", foreignLoc.Ordinal);
            return null;
        }
        if (foreignLoc.Status == null) {
            logger.error("HWI:%n  %s", "foreignLoc.Status cannot be null");
            return null;
        }
        HWInvLoc canonicalFru = new HWInvLoc();

        canonicalFru.ID = foreignLoc.ID;    // + nodeTypeAbbreviation.get(foreignLoc.Type) + foreignLoc.Ordinal;
        canonicalFru.Type = foreignLoc.Type;
        canonicalFru.Ordinal = foreignLoc.Ordinal;
        canonicalFru.Info = foreignLoc.info();

        switch (foreignLoc.Status) {
            case "Empty":
                if (foreignLoc.PopulatedFRU != null) {
                    logger.error("HWI:%n  %s", "Empty slot should have null PopulatedFRU");
                    return null;    // best not put cooked data into the DB
                }
                return canonicalFru;
            case "Populated":
                if (foreignLoc.PopulatedFRU == null) {
                    logger.error("HWI:%n  %s", "PopulatedFRU should not be null");
                    return null;    // best not put cooked bad data into the DB
                }
                canonicalFru.FRUID = foreignLoc.PopulatedFRU.FRUID;
                canonicalFru.FRUType = foreignLoc.PopulatedFRU.Type;
                canonicalFru.FRUSubType = foreignLoc.PopulatedFRU.Subtype;
                canonicalFru.FRUInfo = foreignLoc.PopulatedFRU.info();
                return canonicalFru;
            default:
                logger.error("HWI:%n  Unknown status: %s", foreignLoc.Status);
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

    public String getValue(String json, String name) {
        JsonObject jsonObject;
        try {
            jsonObject = gson.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            logger.fatal("GSON parsing error: %s", e.getMessage());
            return null;
        }
        return jsonObject.get(name).toString();
    }

    private static final Logger logger = LoggerFactory.getInstance("CLIApi", "HWInvTranslator", "console");

//    private static final String emptyPrefix = "empty-";

    private final Gson gson;
    private final HWInvUtil util;
}
