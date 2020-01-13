package com.intel.dai.network_listener;

import java.util.List;

/**
 * Interface defining the behavior of a DAI provider based on network listening (aka network publishing).
 */
public interface NetworkListenerProvider {
    /**
     * A implementation class must convert the incoming data to a list of zero or more CommonDataFormat objects.
     *
     * @param data The string message from the network.
     * @param config The configuration used for the provider implementation.
     * @return The list of zero or more CommonDataFormat objects. Cannot return null but the list can be empty.
     *
     * @throws NetworkListenerProviderException When the provider implementation cannot decode the incoming network message.
     */
    List<CommonDataFormat> processRawStringData(String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException;

    /**
     * A implementation class must perform the required actions for the CommonDataFormat object in the context of the
     * provider. All actions are implemented in the SystemActions implementation object passed into this method. This
     * method cannot throw anything but a fatal exception.
     *
     * @param data The data from the processRawStringData() method above to handle.
     * @param config The configuration used for the provider implementation.
     * @param systemActions The object exposing the actions API for the providers.
     */
    void actOnData(CommonDataFormat data, NetworkListenerConfig config, SystemActions systemActions);
}
