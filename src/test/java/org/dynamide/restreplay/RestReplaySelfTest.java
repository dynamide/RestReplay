/**
 * This document is a part of the source code and related artifacts
 * for CollectionSpace, an open source collections management system
 * for museums and related institutions:
 *
 * http://www.collectionspace.org
 * http://wiki.collectionspace.org
 *
 * Copyright (c) 2009 Regents of the University of California
 *
 * Licensed under the Educational Community License (ECL), Version 2.0.
 * You may not use this file except in compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at
 * https://source.collectionspace.org/collection-space/LICENSE.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.dynamide.restreplay;

import org.dynamide.restreplay.ServiceResult;
import org.dynamide.restreplay.RestReplay;
import org.dynamide.restreplay.RestReplayTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**  The test cases in here also document the ways that RestReplay was designed to be used programmatically.
 *   The most automated way to use RestReplay is demonstrated in runMaster().  You just create a master file and a control
 *   file in the IntegrationTests xml replay repository, which is in services/IntegrationTests + RestReplayTest.RESTREPLAY_REL_DIR_TO_MODULE.
 *
 *   If you choose to run from a module, you'll need to add a dependency to the IntegrationTests module:
 *   e.g.
 *    &lt;project ...>
 *    &lt;dependencies>
 *      &lt;dependency>
            &lt;groupId>org.collectionspace.services&lt;/groupId>
            &lt;artifactId>org.collectionspace.services.IntegrationTests&lt;/artifactId>
            &lt;version>${project.version}&lt;/version>
        &lt;/dependency>
 *
 *   User: laramie
 *   $LastChangedRevision:  $
 *   $LastChangedDate:  $
 */
public class RestReplaySelfTest extends RestReplayTest {

    public static RestReplay createRestReplay() throws Exception {
        return RestReplayTest.createRestReplayUsingIntegrationTestsModule("..");
        //NOTE: this self-test lives in services/IntegrationTests, so relToServicesRoot is ".."
        //      but if you were running from, say, services/dimension/client, then relToServicesRoot would be "../.."
        //      so you would have to call RestReplayTest.createRestReplayUsingIntegrationTestsModule("../..")
        //      which is done for you if you just call RestReplayTest.createRestReplay().
    }

    @Test
    public void runMaster() throws Exception {
        RestReplay replay = createRestReplay();
        List<List<ServiceResult>> list = replay.runMaster("rest-replay-master-self-test.xml");
        logTestForGroup(list, "runMaster");
    }

    @Test
    public void runTestGroup() throws Exception {
        RestReplay replay = createRestReplay();
        replay.readOptionsFromMasterConfigFile("rest-replay-master-self-test.xml"); //or use: RestReplay.DEFAULT_MASTER_CONTROL as master filename;
        replay.setControlFileName("rest-replay-self-test.xml");
        List<ServiceResult> list = replay.runTestGroup("selftestGroup");
        logTest(list, "runTestGroup");
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


    @Test
    public void runTestGroup_AllOptions() throws Exception {
        RestReplay replay = createRestReplay();  //Use the central repository.
        //You can also use your own xml replay repository in your module, like so:
        //   RestReplay replay = RestReplayTest.createRestReplayForModule(); if you are in your module
        //You can also manually specify to use the central repository:
        //   RestReplay replay = RestReplayTest.createRestReplayUsingIntegrationTestsModule("..");  if you are in a module such as dimension
        //   RestReplay replay = RestReplayTest.createRestReplayUsingIntegrationTestsModule("../.."); if you are in a module such as dimension/client

        //You may read Dump, Auths, and protoHostPort from the master file:
        replay.readOptionsFromMasterConfigFile("rest-replay-master-self-test.xml"); //or use: RestReplay.DEFAULT_MASTER_CONTROL as master filename;
        //or you may set those options individually as shown next.
        // Note that controlFileName is NOT set from calling readOptionsFromMasterConfigFile.
        // If you run a master, it sets controlFileName, possibly in a loop.
        // All of the Auths will be read from the master file, and may be referenced from your control file,
        // or you may specify Auths in your control file.  There are also public methods to set the AuthsMap yourself.

        //RestReplay wants to know about two files: a master and a control file
        //  The master references one to many control files.
        //  If you don't call runMaster(), you must specify the control file:
        replay.setControlFileName("rest-replay-self-test.xml");

        //These option default sensibly, some of them from the master, but here's how to set them all:

        //Dump determines how much goes to log, and how verbose.
        RestReplay.Dump dump = RestReplay.getDumpConfig(); //static factory call.
        dump.payloads = false;
        dump.dumpServiceResult = ServiceResult.DUMP_OPTIONS.minimal;
        replay.setDump(dump);

        //use this if you must look it up from some other place.
        // Default is to have it in rest-replay-master.xml
        replay.setProtoHostPort("http://localhost:8180");

        //Default is true, but you can override if you want to leave objects on server, or control the order of deletion.
        replay.setAutoDeletePOSTS(false);

        //You don't need this, but you can inspect what RestReplay holds onto: a data structure of CSIDs
        Map<String, ServiceResult> serviceResultsMap = replay.getServiceResultsMap();

        // ****** RUN A GROUP ***********************************************
        List<ServiceResult> list = replay.runTestGroup("selftestGroup");

        // This runs a group called "organization" inside a control file named above, which happens to be called "organization.xml".
        // You could also run just one test using these options by calling replay.runTest as shown above in RestReplayTest.runOneTest()

        //Now, since we set setAutoDeletePOSTS(false) above, you can clean up manually:
        replay.autoDelete("runTestGroup_AllOptions"); //deletes everything in serviceResultsMap, which it hangs onto.

        logTest(list, "runTestGroup_AllOptions");
    }

}
