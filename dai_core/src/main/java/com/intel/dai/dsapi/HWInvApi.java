package com.intel.dai.dsapi;

import com.intel.dai.exceptions.DataStoreException;
import java.io.IOException;

public interface HWInvApi {
    void initialize();
    int ingest(String inputJsonFileName) throws IOException, InterruptedException, DataStoreException;
}
