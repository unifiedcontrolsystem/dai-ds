package com.intel.dai.ui;

import com.intel.dai.dsapi.Configuration;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.exceptions.ProviderException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.util.Set;

class SystemInfo {

    SystemInfo(Configuration configMgr) {
        _configMgr = configMgr;
        systemInfo = new PropertyMap();
    }

    private PropertyArray extractSchemaFromNodeObject(PropertyMap nodeObject) {
        PropertyArray schema = new PropertyArray();
        Set<String> nodeObjectKeys = nodeObject.keySet();
        for (String key: nodeObjectKeys) {
            PropertyMap columnInfo = new PropertyMap();
            columnInfo.put("data", key);
            columnInfo.put("unit", "string");
            columnInfo.put("heading", key);
            schema.add(columnInfo);
        }
        return schema;
    }
    private PropertyArray extractValuesFromNodes(PropertyArray nodes)
            throws PropertyNotExpectedType, ProviderException{
        PropertyArray data = new PropertyArray();
        for (int node = 0; node < nodes.size(); node++) {
            PropertyMap nodeObject = nodes.getMap(node);
            if (nodeObject == null) {
                throw new ProviderException("'nodeObject' cannot be null!");
            }
            String serviceLocation = nodeObject.getString("LCTN");
            if (serviceLocation == null) {
                throw new ProviderException("'serviceLocation' cannot be null!");
            }
            data.add(nodeObject.values());
        }
        return data;
    }

    private void addNodesOfSpecificNodeType(PropertyArray nodes, String nodeType)
            throws PropertyNotExpectedType, ProviderException {
        if (nodes.size() <= 0) {
            return;
        }
        PropertyMap nodesInfo = new PropertyMap();
        PropertyMap nodeObject = nodes.getMap(0);
        nodesInfo.put("result-data-columns" ,nodeObject.size());
        nodesInfo.put("result-data-lines" ,nodes.size());
        nodesInfo.put("result-status-code" ,0);
        nodesInfo.put("schema", extractSchemaFromNodeObject(nodeObject));
        nodesInfo.put("data", extractValuesFromNodes(nodes));
        systemInfo.put(nodeType, nodesInfo);
    }

    PropertyMap generateSystemInfo() throws DataStoreException, PropertyNotExpectedType,
            ProviderException{
        PropertyArray serviceNodes;
        PropertyArray computeNodes;

        serviceNodes = _configMgr.getServiceNodeConfiguration();
        computeNodes = _configMgr.getComputeNodeConfiguration();

        addNodesOfSpecificNodeType(serviceNodes, "service");
        addNodesOfSpecificNodeType(computeNodes, "compute");
        return systemInfo;
    }

    private Configuration _configMgr;
    private PropertyMap systemInfo;
}
