package org.dynamide.restreplay;

import org.dynamide.util.Tools;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class XmlCompareJdomRepeatingTest {

    private static String getDirectory(){
        String dataDir = "src/main/resources/restreplay/_self_test/XmlCompareJdom";   // this dir lives under service/IntegrationTests
        String pwd = ".";
        try {
            pwd = (new File(".")).getCanonicalPath();
            //System.out.println("pwd in XmlCompareJdomRepeatingTest:"+pwd);
        } catch (Exception e){
            System.err.println("Error trying to find current working directory: "+e);
        }
        String thisDir = Tools.glue(pwd, "/", dataDir);
        //return thisDir;

        return pwd;
    }

    private void testBanner(String msg){
        boolean wantAnnoyingBanner = false;//umm, turn it on if you want the banner, e.g. because surefire is being too quiet.
        if (wantAnnoyingBanner) {
            String BANNER = "-------------------------------------------------------";
            String R = "\r\n";
            System.out.println(BANNER
                    + R + " TEST CLASS: " + this.getClass().getName()
                    + R + " TEST NAME: " + msg
                    + R + " TEST DATA DIR: " + getDirectory()
                    + R
                    + BANNER);
        }
    }

    @Test
    public void testLeftAndRightSame() throws IOException {
        testBanner("testLeftAndRightSame");

        String dir = getDirectory();
        ResourceManager resourceManager = ResourceManager.createRootResourceManager();
        resourceManager.setTestDir(dir);

        String relLeft  = "_self_test/XmlCompareJdom/1-left.xml";
        String relRight = "_self_test/XmlCompareJdom/1-right.xml";
        String expectedPartContent = resourceManager.readResource("testLeftAndRightSame", relLeft, dir+'/'+relLeft).contents;
        String fromServerContent  =  resourceManager.readResource("testLeftAndRightSame", relRight, dir+'/'+relRight).contents;

        String startPath = "/document/*[local-name()='relations-common-list']";
        TreeWalkResults.MatchSpec matchSpec = TreeWalkResults.MatchSpec.createDefault();
        TreeWalkResults results =
            XmlCompareJdom.compareParts(expectedPartContent,
                    "expected",
                    fromServerContent,
                    "from-server",
                    startPath,
                    matchSpec);
        XmlCompareJdomTest.assertTreeWalkResults(results, 1, 0, 0, false, matchSpec);
                                   // addedRight,missingRight,textMismatches,strictMatch,treesMatch
        //System.out.println("testLeftAndRightSame done.  ResourceManager.formatSummaryPlain: "+resourceManager.formatSummaryPlain());
    }

    @Test
    public void testLeftAndRightSameNoStartElement() throws IOException {
        testBanner("testLeftAndRightSameNoStartElement");

        String dir = getDirectory();
        ResourceManager resourceManager = ResourceManager.createRootResourceManager();
        resourceManager.setTestDir(dir);

        String relLeft  = "_self_test/XmlCompareJdom/2-left.xml";
        String relRight = "_self_test/XmlCompareJdom/2-right.xml";
        String expectedPartContent = resourceManager.readResource("testLeftAndRightSameNoStartElement", relLeft, dir+'/'+relLeft).contents;
        String fromServerContent  =  resourceManager.readResource("testLeftAndRightSameNoStartElement", relRight, dir+'/'+relRight).contents;

        String startPath = "/document";
        TreeWalkResults.MatchSpec matchSpec = TreeWalkResults.MatchSpec.createDefault();
        TreeWalkResults results =
            XmlCompareJdom.compareParts(expectedPartContent,
                    "expected",
                    fromServerContent,
                    "from-server",
                    startPath,
                    matchSpec);
        XmlCompareJdomTest.assertTreeWalkResults(results, 0, 0, 0, true, matchSpec);
                                   // addedRight,missingRight,textMismatches,strictMatch,treesMatch
        //System.out.println("testLeftAndRightSameNoStartElement done.  ResourceManager summary: \r\n"+resourceManager.formatSummaryPlain());
    }


}
