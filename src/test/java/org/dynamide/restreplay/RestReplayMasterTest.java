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
        RestReplay replay = createRestReplayForMaven();
        List<List<ServiceResult>> list = replay.runMaster(RestReplay.DEFAULT_MASTER_CONTROL);
        logTestForGroup(list, "RestReplayMasterTest");
    }
}
