package com.intel.dai.inventory;

import com.intel.dai.dsapi.HWInvDbApi;
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.inventory.api.HWInvDiscovery;
import com.intel.dai.inventory.api.HWInvTranslator;
import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClientException;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;

/**
 * This class uses the foreign inventory discovery api and the online inventory api to produce inventory information in
 * canonical form.
 */
public class ForeignInventoryClient {
    public ForeignInventoryClient(Logger logger) {
        log_ = logger;
        hwInvDiscovery_ = new HWInvDiscovery(log_);
    }

    String getCanonicalHWInvJson(String root) {
        return toCanonicalHWInvJson(getForeignHWInvJson(root));
    }

    String getCanonicalHWInvHistoryJson(String startTime) {
        return toCanonicalHWInvHistoryJson(
                getForeignHWInvHistoryJson(startTime));
    }

    String lastHWInventoryHistoryUpdate(HWInvDbApi hwInvDbApi) {
        try {
            return hwInvDbApi.lastHwInvHistoryUpdate();
        } catch (IOException | DataStoreException e) {
            return null;
        }
    }

    /**
     * <p> Converts the HW inventory locations in foreign format into canonical format. </p>
     * @param foreignHWInvJson json containing the HW inventory in foreign format
     * @return json containing the HW inventory in canonical format
     */
    private String toCanonicalHWInvJson(String foreignHWInvJson) {
        if (foreignHWInvJson == null) return null;

        HWInvTranslator tr = new HWInvTranslator(new HWInvUtilImpl(log_));
        ImmutablePair<String, String> canonicalHwInv = tr.foreignToCanonical(foreignHWInvJson);
        if (canonicalHwInv.getKey() == null) {
            log_.error("failed to translate foreign HW inventory json");
            return null;
        }
        return canonicalHwInv.getValue();
    }

    private String toCanonicalHWInvHistoryJson(String foreignHWInvJson) {
        if (foreignHWInvJson == null) return null;

        HWInvTranslator tr = new HWInvTranslator(new HWInvUtilImpl(log_));
        ImmutablePair<String, String> canonicalHwInv = tr.foreignToCanonical(foreignHWInvJson);
        if (canonicalHwInv.getKey() == null) {
            log_.error("failed to translate foreign HW inventory json");
            return null;
        }
        return canonicalHwInv.getValue();
    }

    /**
     * <p> Obtains the HW inventory locations at the given root.  If the root is "", all locations of the
     * HPC is returned. </p>
     * @param root root location for a HW inventory tree or "" for the root of the entire HPC
     * @return json containing the requested locations
     */
    private String getForeignHWInvJson(String root) {
        if (root == null) return null;

        try {
            hwInvDiscovery_.initialize();
            log_.info("rest client created");

        } catch (RESTClientException e) {
            log_.fatal("Fail to create REST client: %s", e.getMessage());
            return null;
        }

        ImmutablePair<Integer, String> foreignHwInv;
        if (root.equals("")) {
            foreignHwInv = hwInvDiscovery_.queryHWInvTree();
        } else {
            foreignHwInv = hwInvDiscovery_.queryHWInvTree(root);
        }
        if (foreignHwInv.getLeft() != 0) {
            log_.error("failed to acquire foreign HW inventory json");
            return null;
        }
        return foreignHwInv.getRight();
    }

    private String getForeignHWInvHistoryJson(String startTime) {
        try {
            hwInvDiscovery_.initialize();
            log_.info("rest client created");

        } catch (RESTClientException e) {
            log_.fatal("Fail to create REST client: %s", e.getMessage());
            return null;
        }

        ImmutablePair<Integer, String> foreignHwInvHistory = hwInvDiscovery_.queryHWInvHistory(startTime);

        if (foreignHwInvHistory.getLeft() != 0) {
            log_.error("failed to acquire foreign HW inventory history json");
            return null;
        }
        return foreignHwInvHistory.getRight();
    }

    private final Logger log_;
    private HWInvDiscovery hwInvDiscovery_;
}
