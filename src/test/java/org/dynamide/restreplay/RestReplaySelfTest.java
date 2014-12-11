package org.dynamide.restreplay;

import org.dynamide.restreplay.server.EmbeddedServer;
import org.dynamide.util.Tools;
import org.testng.annotations.Test;
import java.util.List;

/**  The test cases in here also document the ways that RestReplay was designed to be used programmatically.
 *   The most automated way to use RestReplay is demonstrated in runMaster().  You just create a master file and a control
 *   file in the IntegrationTests xml replay repository, which is in RestReplayTest.RESTREPLAY_REL_DIR_TO_MODULE.
 */
public class RestReplaySelfTest extends RestReplayTest {

    public static Master createRestReplay() throws Exception {
        //return RestReplayTest.createRestReplayUsingIntegrationTestsModule("..");
        return createRestReplayMasterForMaven();

        //NOTE: this self-test lives in services/IntegrationTests, so relToServicesRoot is ".."
        //      but if you were running from, say, services/dimension/client, then relToServicesRoot would be "../.."
        //      so you would have to call RestReplayTest.createRestReplayUsingIntegrationTestsModule("../..")
        //      which is done for you if you just call RestReplayTest.createRestReplay().
    }

    @Test
    public void runMaster() throws Exception {
        String port = ""+EmbeddedServer.DEFAULT_PORT;
        String fromProps = System.getProperty("port");
        if (Tools.notEmpty(fromProps)) {
            port = fromProps;
        }
        EmbeddedServer server = new EmbeddedServer();
        server.startServer(port);
        try {
            Master master = createRestReplay();
            master.getVars().put("SELFTEST_PORT", port);

            List<List<ServiceResult>> list = master.runMaster("_self_test/master-self-test.xml");
            logTestForGroup(list, "runMaster");
        } finally {
            server.stopServer();
        }
    }

/*
    @Test
    public void runOneTest() throws Exception {
        RestReplay replay = createRestReplay();
        replay.readOptionsFromMasterConfigFile("rest-replay-master-self-test.xml");
        replay.setControlFileName("rest-replay-self-test.xml");

        ServiceResult res = replay.runTest("selftestGroup", "OrgAuth1");
        logTest(res, "runOneTest");
    }


    @Test
    public void runMultipleTestsManualCleanup() throws Exception {
        RestReplay replay = createRestReplay();
        replay.readOptionsFromMasterConfigFile("rest-replay-master-self-test.xml");
        replay.setControlFileName("rest-replay-self-test.xml");
        replay.setAutoDeletePOSTS(false);  //defaults to true, so turn it off to to it ourselves.

        List<ServiceResult> testResultsList = new ArrayList<ServiceResult>();

        ServiceResult res1 = replay.runTest("selftestGroup", "OrgAuth1");
        testResultsList.add(res1);

        ServiceResult res2 = replay.runTest("selftestGroup", "Org1");
        testResultsList.add(res2);

        ServiceResult res3 = replay.runTest("selftestGroup", "getOrg1");
        testResultsList.add(res3);

        //Now, clean up.  You may skip this if your tests do all the DELETEs.
        List<ServiceResult> deleteList = replay.autoDelete("runMultipleTestsManualCleanup");

        logTest(testResultsList, "runTwoTestsManualCleanup.tests");
        logTest(deleteList, "runTwoTestsManualCleanup.cleanups");

    }
*/

/*
    //@Test
    public void runTestGroup_AllOptions() throws Exception {
        RestReplay replay = createRestReplay();  //Use the central repository.
        //You can also use your own xml replay repository in your module, like so:
        //   RestReplay replay = RestReplayTest.createRestReplayForModule(); if you are in your module
        //You can also manually specify to use the central repository:
        //   RestReplay replay = RestReplayTest.createRestReplayUsingIntegrationTestsModule("..");  if you are in a module such as dimension
        //   RestReplay replay = RestReplayTest.createRestReplayUsingIntegrationTestsModule("../.."); if you are in a module such as dimension/client

        //You may read Dump, Auths, and protoHostPort from the master file:
        replay.readOptionsFromMasterConfigFile("_self_test/master-self-test.xml"); //or use: RestReplay.DEFAULT_MASTER_CONTROL as master filename;
        //or you may set those options individually as shown next.
        // Note that controlFileName is NOT set from calling readOptionsFromMasterConfigFile.
        // If you run a master, it sets controlFileName, possibly in a loop.
        // All of the Auths will be read from the master file, and may be referenced from your control file,
        // or you may specify Auths in your control file.  There are also public methods to set the AuthsMap yourself.

        //RestReplay wants to know about two files: a master and a control file
        //  The master references one to many control files.
        //  If you don't call runMaster(), you must specify the control file:
        replay.setControlFileName("_self_test/self-test.xml");

        //These option default sensibly, some of them from the master, but here's how to set them all:

        //Dump determines how much goes to log, and how verbose.
        RestReplay.Dump dump = RestReplay.getDumpConfig(); //static factory call.
        dump.payloads = false;
        dump.dumpServiceResult = ServiceResult.DUMP_OPTIONS.minimal;
        replay.setDump(dump);

        //use this if you must look it up from some other place.
        // Default is to have it in master.xml
        replay.setProtoHostPort("http://localhost:8180");

        //Default is true, but you can override if you want to leave objects on server, or control the order of deletion.
        replay.setAutoDeletePOSTS(false);

        //You don't need this, but you can inspect what RestReplay holds onto: a data structure of CSIDs
        Map<String, ServiceResult> serviceResultsMap = replay.getServiceResultsMap();

        // ****** RUN A GROUP ***********************************************
        List<ServiceResult> list = replay.runTestGroup("selftestGroup");

        // This runs a group called "organization" inside a control file named above, which happens to be called "organization.xml".
        // You could also run just one test using these options by calling replay.runTest as shown above in RestReplayTest.runOneTest()

        //TODO: this has a problem: it will override a master file's autodelete, and if there is a bad location header in a response, then this will
        // hang, and longer than the timeout is for, for some reason.
        //  autodelete logs better, and the timeout is working:
        //Now, since we set setAutoDeletePOSTS(false) above, you can clean up manually:
        replay.autoDelete("runTestGroup_AllOptions"); //deletes everything in serviceResultsMap, which it hangs onto.

        logTest(list, "runTestGroup_AllOptions");
    }
*/
    public static void main(String[] args)
    throws  Exception {
        new RestReplaySelfTest().runMaster();
    }
}
