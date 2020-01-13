package com.intel.dai.inventory.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.intel.dai.dsapi.HWInvSlot;
import com.intel.dai.dsapi.HWInvTree;
import com.intel.dai.dsapi.HWInvUtil;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HWInvTranslator {
    private static Logger logger = LoggerFactory.getInstance("CLIApi", "HWInvTranslator", "console");

    private static final Map<String, String> nodeTypeAbbreviation;
    private static final String emptyPrefix = "empty-";

    static {
        nodeTypeAbbreviation = new HashMap<>();
        nodeTypeAbbreviation.put("Cabinet", "x");
        nodeTypeAbbreviation.put("Chassis", "c");
        nodeTypeAbbreviation.put("ComputeModule", "s"); // best guess - insufficient information in spec
        nodeTypeAbbreviation.put("NodeEnclosure", "b"); // best guess - insufficient information in spec
        nodeTypeAbbreviation.put("Node", "n");
        nodeTypeAbbreviation.put("Memory", "d");
        nodeTypeAbbreviation.put("Processor", "p");
    }

    private transient Gson gson;
    private transient String inputFileName;
    private transient String outputFileName;
    private transient HWInvUtil util;

    HWInvTranslator(String inputFileName, String outputFileName, HWInvUtil util) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.util = util;

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();
    }

    /**
     * Parses a node JSON format
     * Implicit Format: FullyFlat or Hierarchical
     * The hierarchical parser can handle the other format.  So, only one parser is necessary.
     * Since this format has no defining feature, so this parser should be used only when the other parsers defined
     * below have failed.
     *
     * @return
     * @throws IOException
     * @throws JsonIOException
     * @throws JsonSyntaxException
     */
    public ForeignHWInvByLocNode toForeignHWInvByLocNode() throws
            IOException, JsonIOException, JsonSyntaxException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFileName),
                StandardCharsets.UTF_8));
        return gson.fromJson(br, ForeignHWInvByLocNode.class);
    }

    /**
     * Parses an array of FullyFlat ForeignHWInvByLocNodes.
     * The defining feature is the json is an array.
     *
     * @return
     * @throws IOException
     * @throws JsonIOException
     * @throws JsonSyntaxException
     */
    public ForeignHWInvByLoc[] toForeignHWInvByLocList() throws
            IOException, JsonIOException, JsonSyntaxException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFileName),
                StandardCharsets.UTF_8));
        return gson.fromJson(br, ForeignHWInvByLoc[].class);
    }

    /**
     * Parses a structure of named lists of ForeignHWInvByLocNodes.
     * Explicit Format: FullyFlat, Hierarchical or NestNodesOnly
     * The defining attribute is "Format".
     *
     * @return
     * @throws IOException
     * @throws JsonIOException
     * @throws JsonSyntaxException
     */
    public ForeignHWInventory toForeignHWInventory() throws
            IOException, JsonIOException, JsonSyntaxException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFileName),
                StandardCharsets.UTF_8));
        return gson.fromJson(br, ForeignHWInventory.class);
    }
    /**
     * <p> This method performs JSON to JSON translation of a Foreign HW Inventory tree to its
     * canonical representation.
     * </p>
     * @since 1.0.0
     */
    int foreignToCanonical() {
        try {
            HWInvTree canonicalTree;
            try {
                ForeignHWInvByLoc[] frus = toForeignHWInvByLocList();
                logger.info("Parsed HW Loc List");
                canonicalTree = toCanonical(frus);
            } catch (JsonIOException | JsonSyntaxException e) {
                ForeignHWInventory foreignTree = toForeignHWInventory();
                if (foreignTree.XName != null) {
                    if (foreignTree.Format == null) {
                        logger.error("Missing format field");
                        return 1;
                    }
                    logger.info("Parsed HW Inventory");
                    canonicalTree = toCanonical(foreignTree);
                } else {
                    ForeignHWInvByLocNode node = toForeignHWInvByLocNode();
                    if (!node.Type.equals("Node")) {  // only support node for now
                        logger.error("Unsupported hw Loc type: %s", node.Type);
                        return 1;
                    }
                    logger.info("Parsed HW Loc Node");
                    canonicalTree = toCanonical(node);
                }
            }
            String canonicalJson = util.toCanonicalJson(canonicalTree);
            util.fromStringToFile(canonicalJson, outputFileName);
        } catch (Exception e) {
            logger.error("Conversion failed: Exception: %s", e.getMessage());
            return 1;
        }
        return 0;
    }

    /**
     * I decided not to implement this using reflection for now.  Using reflection may
     * make the parser too general.  However, it may reduce the number of lines of code.
     *
     * @param node
     * @return
     */
    HWInvTree toCanonical(ForeignHWInvByLocNode node) {
        HWInvTree canonicalTree = new HWInvTree();
        ArrayList<ForeignHWInvByLocNode> singletonNode = new ArrayList<>();
        singletonNode.add(node);
        addForeignNodesToCanonical(singletonNode, canonicalTree);
        return canonicalTree;
    }
    HWInvTree toCanonical(ForeignHWInvByLoc[] frus) {
        HWInvTree canonicalTree = new HWInvTree();
        addForeignFlatFRUToCanonical(
                Arrays.stream(frus).collect(Collectors.toList()),
                canonicalTree);
        return canonicalTree;
    }
    /**
     * <p> This method performs POJO to POJO translation of a Foreign HW Inventory tree to its
     * canonical representation.
     * </p>
     * @param foreignTree the Foreign POJO containing the HW inventory
     * @return the canonical POJO containing the HW inventory
     * @since 1.0.0
     */
    HWInvTree toCanonical(ForeignHWInventory foreignTree) {
        HWInvTree canonicalTree = new HWInvTree();

        addForeignCabinetsCanonical(foreignTree.Cabinets, canonicalTree);
        addForeignChassisToCanonical(foreignTree.Chassis, canonicalTree);
        addForeignComputeModulesToCanonical(foreignTree.ComputeModules, canonicalTree);
        addForeignRouterModulesToCanonical(foreignTree.RouterModules, canonicalTree);

        addForeignNodeEnclosuresToCanonical(foreignTree.NodeEnclosures, canonicalTree);
        addForeignHSNBoardsToCanonical(foreignTree.HSNBoards, canonicalTree);
        addForeignNodesToCanonical(foreignTree.Nodes, canonicalTree);
        addForeignProcessorsToCanonical(foreignTree.Processors, canonicalTree);

        addForeignMemoryToCanonical(foreignTree.Memory, canonicalTree);
        addForeignCabinetPDUToCanonical(foreignTree.CabinetPDU, canonicalTree);
        addForeignCabinetPDUOutletsToCanonical(foreignTree.CabinetPDUOutlets, canonicalTree);

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
     * Add only the FRU denoted by the vertex to the specified canonical tree.  Their children will be added by other
     * code.
     *
     * @param foreignFRUList
     * @param canonicalTree
     * @return number of entries added to the DB.
     */
    private <T> int addForeignFRUsNotChildrenToCanonical(List<T> foreignFRUList,
                                                         HWInvTree canonicalTree) {
        List<ForeignHWInvByLoc> flatFrus = downcastToForeignHWInvByLocList(foreignFRUList);
        return addForeignFlatFRUToCanonical(flatFrus, canonicalTree);
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

    private int addForeignFlatFRUToCanonical(List<ForeignHWInvByLoc> foreignLocList,
                                             HWInvTree canonicalTree) {
        if (foreignLocList == null) {
            return 0;
        }

        int numSlotsAdded = 0;
        for (ForeignHWInvByLoc loc: foreignLocList) {
            HWInvSlot slot = toCanonical(loc);
            if (slot == null) {
                continue;   //ignore failed conversions
            }
            logger.info("Adding %s", slot.toString());
            canonicalTree.FRUS.add(slot);
            numSlotsAdded++;
        }
        return numSlotsAdded;
    }
    HWInvSlot toCanonical(ForeignHWInvByLoc foreignLoc) {
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
        HWInvSlot canonicalFru = new HWInvSlot();

        canonicalFru.ID = foreignLoc.ID;    // + nodeTypeAbbreviation.get(foreignLoc.Type) + foreignLoc.Ordinal;
        canonicalFru.ParentID = extractParentId(foreignLoc.ID);
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
}
