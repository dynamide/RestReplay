package org.dynamide.restreplay;

import java.util.List;

public class RestReplayMasterTest  extends RestReplayTest {

    //@Test
    public void runMaster() throws Exception {
        Master master = createRestReplayMasterForMaven();
        List<List<ServiceResult>> list = master.runMaster(Master.DEFAULT_MASTER_CONTROL);
        logTestForGroup(list, "RestReplayMasterTest");
    }
}
