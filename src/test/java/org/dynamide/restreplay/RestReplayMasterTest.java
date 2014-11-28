package org.dynamide.restreplay;

import org.dynamide.restreplay.ServiceResult;
import org.dynamide.restreplay.RestReplay;
import org.dynamide.restreplay.RestReplayTest;
import org.testng.annotations.Test;

import java.util.List;

/**
 * User: laramie
 * $LastChangedRevision:  $
 * $LastChangedDate:  $
 */
public class RestReplayMasterTest  extends RestReplayTest {

    //@Test
    public void runMaster() throws Exception {
        //RestReplay replay = createRestReplayUsingIntegrationTestsModule("..");
        Master master = createRestReplayMasterForMaven();
        List<List<ServiceResult>> list = master.runMaster(Master.DEFAULT_MASTER_CONTROL);
        logTestForGroup(list, "RestReplayMasterTest");
    }
}
