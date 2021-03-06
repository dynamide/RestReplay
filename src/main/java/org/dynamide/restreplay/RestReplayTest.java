package org.dynamide.restreplay;

import org.dynamide.util.Tools;
import org.testng.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Subclass this test to programmatically control RestReplay from a surefire test.  See example in IntegrationTests :: RestReplaySelfTest
 * User: laramie
 * $LastChangedRevision:  $
 * $LastChangedDate:  $
 */
public class RestReplayTest {

    public static final String RESTREPLAY_REL_DIR_TO_MODULE = "/src/main/resources/restreplay";
    public static final String REPORTS_DIRNAME = "reports";
    public static final String RESTREPLAY_REL_DIR_REPORTS_TO_MODULE= "/"+REPORTS_DIRNAME;

    public static RestReplay createRestReplayForMaven() throws Exception {
        String baseDir = getTestDirectory();
        ResourceManager rootResourceManager = ResourceManager.createRootResourceManager();
        rootResourceManager.setTestDir(baseDir);
        RestReplay replay = new RestReplay(baseDir+RESTREPLAY_REL_DIR_TO_MODULE,
                                           baseDir+RESTREPLAY_REL_DIR_REPORTS_TO_MODULE,
                                           rootResourceManager,
                                           null);
        return replay;
    }

    public static Master createRestReplayMasterForMaven() throws Exception {
        String baseDir = getTestDirectory();
        ResourceManager rootResourceManager = ResourceManager.createRootResourceManager();
        rootResourceManager.setTestDir(baseDir);
        Master master = new Master(baseDir+RESTREPLAY_REL_DIR_TO_MODULE,
                baseDir+RESTREPLAY_REL_DIR_REPORTS_TO_MODULE,
                rootResourceManager);
        return master;
    }

    private static String getTestDirectory(){
        String pwd = ".";
        try {
            pwd = (new File(".")).getCanonicalPath();
            //System.out.println("pwd in createRestReplayForMaven:"+pwd);
        } catch (Exception e){
            System.err.println("Error trying to find current working directory: "+e);
        }
        return pwd;
    }

    public static void logTest(ServiceResult sresult, String testname){
        ResultSummary summary = resultSummary(sresult, HTML);
        org.testng.Reporter.log(summary.table);
        System.out.println(""+summary.oks+'/'+summary.total+" PASSED.  See Output from test '"+testname+"'. Error tests: "+summary.errorTests);
    }

    public static void logTest(List<ServiceResult> list, String testname){
        ResultSummary summary = resultSummary(list, HTML);
        org.testng.Reporter.log(summary.table);
        System.out.println(""+summary.oks+'/'+summary.total+" PASSED.  See Output from test '"+testname+"'. Error tests: "+summary.errorTests);
    }

    public static void logTestForGroup(List<List<ServiceResult>> list, String testname){
        ResultSummary summary = resultSummaryForGroup(list, HTML);
        org.testng.Reporter.log(summary.table);
        ResultSummary textSummary = resultSummaryForGroup(list, TEXT);
        //System.out.println("\n\n\n\n\n\nSkipping SUMMARY.");
        //System.out.println("\n\n\n\n\n\n============== SUMMARY==============");
        //System.out.println("SUMMARY: "+textSummary.table);
        //System.out.println("==============END SUMMARY===============");
        //System.out.println("TESTS COMPLETE.  "+summary.oks+'/'+summary.total+" PASSED.  See Output from test '" + testname + "'. Error tests: " + summary.errorTests);
    }


    //============== HELPERS AND FORMATTING =====================================================
    public static class FORMAT {
        private static final String TBLSTART = "";
        private static final String ROWSTART = "\r\n       ";
        private static final String ROWSTARTRED = "\r\n    ** ";
        private static final String SEP = "  |  ";
        private static final String ROWEND = "";
        private static final String ROWENDRED = "";
        private static final String TBLEND = "";

    }
    public static final FORMAT TEXT = new FORMAT();
    public static class HTML_FORMAT extends FORMAT {
        private static final String TBLSTART = "<table border='1'>";
        private static final String ROWSTART = "<tr><td bgcolor='white'>";
        private static final String ROWSTARTRED = "<tr><td bgcolor='red'><b>";
        private static final String SEP = "</td><td>";
        private static final String ROWEND = "</td></tr>";
        private static final String ROWENDRED = "</b></td></tr>";
        private static final String TBLEND = "</table>";
    }
    public static final FORMAT HTML = new HTML_FORMAT();

    public static class ResultSummary {
        public long oks = 0;
        public long total = 0;
        public String table = "";
        public List<String> groups = new ArrayList<String>();
        public List<String> errorTests = new ArrayList<String>();
    }

    public static ResultSummary resultSummaryForGroup(List<List<ServiceResult>> list, FORMAT format){
        ResultSummary summary = new ResultSummary();
        summary.oks = 0;
        summary.total = 0;
        StringBuffer buff = new StringBuffer();
        buff.append(format.TBLSTART);
        for (List<ServiceResult> serviceResults : list){
            String groupID = "";
            if (serviceResults.size()>0){
                groupID = serviceResults.get(0).testGroupID;
                summary.groups.add(groupID);
            }
            buff.append(format.ROWSTART+"RestReplay testGroup:"+groupID+format.ROWEND);
            for (ServiceResult serviceResult : serviceResults){
                summary.total++;
                if (serviceResult.gotExpectedResult()){
                    summary.oks++;
                    buff.append(format.ROWSTART+serviceResult.minimal()+format.ROWEND);
                } else {
                    buff.append(format.ROWSTARTRED+serviceResult.minimal()+format.ROWENDRED);
                    summary.errorTests.add(serviceResult.testGroupID+':'+serviceResult.testID+':'+serviceResult.fullURL);
                }
            }
        }
        buff.append(format.TBLEND);
        summary.table = buff.toString();
        return summary;
    }

    public static ResultSummary resultSummary(List<ServiceResult> serviceResults,  FORMAT format){
        ResultSummary summary = new ResultSummary();
        summary.oks = 0;
        summary.total = 0;
        StringBuffer buff = new StringBuffer();
        buff.append(format.TBLSTART);
        for (ServiceResult serviceResult : serviceResults){
            summary.total++;
            if (serviceResult.gotExpectedResult()){
                summary.oks++;
                buff.append(format.ROWSTART+serviceResult.minimal()+format.ROWEND);
            } else {
                buff.append(format.ROWSTARTRED+serviceResult.minimal()+format.ROWENDRED);
            }
        }
        buff.append(format.TBLEND);
        summary.table = buff.toString();
        return summary;
    }

    public static ResultSummary resultSummary(ServiceResult serviceResult, FORMAT format){
        ResultSummary summary = new ResultSummary();
        summary.oks = 0;
        summary.total = 1;
        StringBuffer buff = new StringBuffer();
        buff.append(format.TBLSTART);
        if (serviceResult.gotExpectedResult()){
            summary.oks = 1;
            buff.append(format.ROWSTART+serviceResult.minimal()+format.ROWEND);
        } else {
            buff.append(format.ROWSTARTRED+serviceResult.minimal()+format.ROWENDRED);
        }
        buff.append(format.TBLEND);
        summary.table = buff.toString();
        return summary;
    }

}
