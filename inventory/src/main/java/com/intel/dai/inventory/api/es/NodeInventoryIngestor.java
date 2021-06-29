// TODO: Absorb into dai_core

package com.intel.dai.inventory.api.es;

import com.google.gson.Gson;
import com.intel.dai.dsapi.pojo.Dimm;
import com.intel.dai.dsapi.pojo.FruHost;
import com.intel.dai.dsapi.pojo.NodeInventory;
import org.voltdb.VoltTable;
import org.voltdb.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeInventoryIngestor {
    private final static Gson gson = new Gson();
    Client client = null;
    ClientConfig config = null;

    void connect(String server, int port) throws IOException {
        config = new ClientConfig("", "");
        client = ClientFactory.createClient(config);
        client.createConnection(server, port);
        System.out.println("Connected to voltdb");
    }

    List<FruHost> enumerateFruHosts() {
        try {
            ClientResponse cr = client.callProcedure("Get_FRU_Hosts");
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                System.err.println(cr.getStatusString());
                return null;
            }
            VoltTable tuples = cr.getResults()[0];
            System.out.println(tuples.getRowCount());
            tuples.resetRowPosition();
            ArrayList<FruHost> fruHosts = new ArrayList<>();
            while (tuples.advanceRow()) {
                String source = tuples.getString(3);
//                System.out.println(source);
                fruHosts.add(gson.fromJson(source, FruHost.class));
            }
            return fruHosts;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (ProcCallException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    void constructAndIngestNodeInventoryJson(FruHost fruHost) throws IOException, ProcCallException {
        System.out.printf("Constructing node inventory from %s\n", fruHost.hostname);
        NodeInventory nodeInventory = new NodeInventory(fruHost);

        Map<String, String> dimmJsons = getDimmJsonsOnFruHost(fruHost.mac);
        for (String location : dimmJsons.keySet()) {
            addDimmJsonsToFruHostJson(nodeInventory, location, dimmJsons.get(location));
        }

//        insertNodeInventoryHistory(nodeInventory);    // TODO: refactor to use dai_core
    }

    void addDimmJsonsToFruHostJson(NodeInventory nodeInventory, String location, String json) {
//        System.out.printf("  Adding %s => %s\n", location, json);
        switch (location) {
            case "CPU0_DIMM_A1":
                nodeInventory.CPU0_DIMM_A1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_B1":
                nodeInventory.CPU0_DIMM_B1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_C1":
                nodeInventory.CPU0_DIMM_C1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_D1":
                nodeInventory.CPU0_DIMM_D1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_E1":
                nodeInventory.CPU0_DIMM_E1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_F1":
                nodeInventory.CPU0_DIMM_F1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_G1":
                nodeInventory.CPU0_DIMM_G1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_H1":
                nodeInventory.CPU0_DIMM_H1 = gson.fromJson(json, Dimm.class);
                break;

            case "CPU1_DIMM_A1":
                nodeInventory.CPU1_DIMM_A1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_B1":
                nodeInventory.CPU1_DIMM_B1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_C1":
                nodeInventory.CPU1_DIMM_C1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_D1":
                nodeInventory.CPU1_DIMM_D1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_E1":
                nodeInventory.CPU1_DIMM_E1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_F1":
                nodeInventory.CPU1_DIMM_F1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_G1":
                nodeInventory.CPU1_DIMM_G1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_H1":
                nodeInventory.CPU1_DIMM_H1 = gson.fromJson(json, Dimm.class);
                break;

            default:
                System.err.printf("Unknown location %s\n", location);
        }
    }

    Map<String, String> getDimmJsonsOnFruHost(String fruHostMac) {
        try {
            ClientResponse cr = client.callProcedure("Get_Dimms_on_FRU_Host", fruHostMac);
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                System.err.println(cr.getStatusString());
                return null;
            }
            VoltTable tuples = cr.getResults()[0];
//            System.out.println(tuples.getRowCount());
            tuples.resetRowPosition();
            HashMap<String, String> dimmMap = new HashMap<>();
            while (tuples.advanceRow()) {
//                String serial = tuples.getString(0);
//                String mac = tuples.getString(1);
//                long timestamp = tuples.getTimestampAsLong(2);
                String locator = tuples.getString(3);
                String source = tuples.getString(4);
//                long DbUpdatedTimestamp = tuples.getTimestampAsLong(5);

//                System.out.println(source);
                dimmMap.put(locator, source);
            }
            return dimmMap;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (ProcCallException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
