package org.dynamide.restreplay;

import org.apache.commons.cli.*;
import org.dynamide.restreplay.server.EmbeddedServer;
import org.dynamide.util.Tools;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This is the main entry point to RestReplay
 * from the command line, from running the jar file,
 * or for launching the self-test with its self-test web server.
 * This class, <code>Main</code>,
 * is marked as the entry point in the ResReplay jar file distribution, so running the jar file
 * runs this class, passing all the jar command line options here.
 * </p>
 *
 * <p>Main checks {@see org.dynamide.restreplay.RunOptions}, and runs an instance of
 *  {@see org.dynamide.restreplay.Master} to run master files, and  {@see org.dynamide.restreplay.RestReplay}
 *  to run control files.
 * </p>
 *
 * @author Laramie Crocker
 */
public class Main {

    private static String opt(CommandLine line, String option) {
        String result;
        String fromProps = System.getProperty(option);
        if (Tools.notEmpty(fromProps)) {
            return fromProps;
        }
        if (line == null) {
            return "";
        }
        result = line.getOptionValue(option);
        if (result == null) {
            result = "";
        }
        return result;
    }

    //======================== MAIN ===================================================================

    /*  From javadoc for Options.addOption():
            opt - Short single-character name of the option.
            longOpt - Long multi-character name of the option.
            hasArg - flag signally if an argument is required after this option
            description - Self-documenting description
     */

    private static Options createOptions() {
        Options options = new Options();
        options.addOption(     "help", false, "RestReplay Help");
        options.addOption("h", false, "RestReplay Help");
        options.addOption(     "selftest", false, "RestReplay selftest");
        options.addOption(     "pause", false, "RestReplay pause before selftest");
        options.addOption(     "noexit", false, "RestReplay pause before exit");
        options.addOption(     "port", true, "RestReplay selftest port");
        options.addOption("d", "testdir", true, "default/testdir");
        options.addOption("r", "reports", true, "default/reports");
        options.addOption("g", "testGroup", true, "default/testGroup");
        options.addOption("t", "test", true, "default/test");
        options.addOption("e", "env", true, "e.g. dev");
        options.addOption(     "autoDeletePOSTS", true, "true deletes POSTs after each testgroup");
        options.addOption(     "dumpResults", true, "dumpResults to stdout");
        options.addOption("c", "control", true, "control filename");
        options.addOption("m", "master", true, "master filename");

        return options;
    }

    //TODO: this is obviated......Need to set the default arg names in Apache https://commons.apache.org/proper/commons-cli/apidocs/org/apache/commons/cli/HelpFormatter.html
    public static String usage() {
        String result = "org.dynamide.restreplay.RestReplay {args}\r\n"
                + " args: \r\n"
                + "  -d|-testdir <dir> \r\n"
                + "  -r|-reports <dir> \r\n"
                + "  -m|-master <filename> \r\n"
                + "  -c|-control <filename> \r\n"
                + "  -g|-testGroup <ID> \r\n"
                + "  -t|-test <ID> \r\n"
                + "  -e|-env <ID> \r\n"
                + "  -dumpResults true|false \r\n"
                + "  -autoDeletePOSTS true|false \r\n"
                + "  -selftest \r\n"
                + "  -pause \r\n"
                + "  -port <selftest-server-port>\r\n"
                + "   \r\n"
                + " Note: -DautoDeletePOSTS won't force deletion if set to false in control file."
                + "   \r\n"
                + "   \r\n"
                + " You may also override these program args with system args, e.g.: \r\n"
                + "   -Dtestdir=/path/to/dir \r\n"
                + "   \r\n";
        return result;
    }

    public static void printHelp(Options options){
        String header = "Run a RestReplay test, control, or master\n\n";
        String footer = "\nDocumentation at https://github.com/dynamide/RestReplay";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("RestReplay", header, options, footer, true);

        System.out.println("\r\n"
                + " Note: -DautoDeletePOSTS won't force deletion if set to false in control file."
                + "   \r\n"
                + "   \r\n"
                + " You may also override these program args with system args, e.g.: \r\n"
                + "   -Dtestdir=/path/to/dir \r\n"
                + "   \r\n");
    }


    private static void pause(String msg) throws IOException {
        DataInputStream in = new DataInputStream(System.in);
        System.out.println(msg);
        byte b = in.readByte();
        char ch = (char) b;
        System.out.println("Char : " + ch);
    }

    public static void main(String[] args) throws Exception {
        Options options = createOptions();
        EmbeddedServer selfTestServer = null;
        //System.out.println("System CLASSPATH: "+prop.getProperty("java.class.path", null));
        CommandLineParser parser = new GnuParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            String testdir = opt(line, "testdir");
            String reportsDir = opt(line, "reports");
            String testGroupID = opt(line, "testGroup");
            String testID = opt(line, "test");
            String envID = opt(line, "env");
            String autoDeletePOSTS = opt(line, "autoDeletePOSTS");
            String dumpResultsFromCmdLine = opt(line, "dumpResults");
            String controlFilename = opt(line, "control");
            String restReplayMaster = opt(line, "master");
            String selfTestPort = opt(line, "port");
            String pause = opt(line, "pause");
            String noexit = opt(line, "noexit");
            if (line.hasOption('h') || line.hasOption("help")){
                //System.out.println(usage());
                printHelp(options);
                System.exit(0);
            }
            if (line.hasOption("pause")){
                pause("Start debugging tool, then press Enter to resume.");
            }

            if (line.hasOption("selftest")){
                //restReplayMaster = "_self_test/master-self-test.xml";
                if (Tools.isBlank(selfTestPort)){
                    selfTestPort = ""+EmbeddedServer.DEFAULT_PORT;
                }
                selfTestServer = new EmbeddedServer();
                selfTestServer.startServer(selfTestPort);
                System.out.println("selftest server started on port: "+selfTestPort);
            }

            if (Tools.isBlank(reportsDir)) {
                reportsDir = testdir + '/' + RestReplayTest.REPORTS_DIRNAME;
            }
            reportsDir = Tools.fixFilename(reportsDir);
            testdir = Tools.fixFilename(testdir);
            controlFilename = Tools.fixFilename(controlFilename);

            boolean bAutoDeletePOSTS = true;
            if (Tools.notEmpty(autoDeletePOSTS)) {
                bAutoDeletePOSTS = Tools.isTrue(autoDeletePOSTS);
            }
            boolean bDumpResults = false;
            if (Tools.notEmpty(dumpResultsFromCmdLine)) {
                bDumpResults = Tools.isTrue(dumpResultsFromCmdLine);
            }
            if (Tools.isEmpty(testdir)) {
                System.err.println("ERROR: testdir was not specified.");
                return;
            }
            File f = null, fMaster = null;
            String fMasterPath = "";
            if (Tools.isEmpty(restReplayMaster)) {
                if (Tools.isEmpty(controlFilename)) {
                    System.err.println("Exiting.  No Master file (empty) and Control file not found (empty)");
                    return;
                }
                f = new File(Tools.glue(testdir, "/", controlFilename));
                if (!f.exists()) {
                    System.err.println("Control file not found: " + f.getCanonicalPath());
                    return;
                }
            } else {
                if (selfTestServer==null) {
                    //if running selfTestServer, then it loads from classpath.  Only construct full path if NOT doing selftest.
                    fMaster = new File(Tools.glue(testdir, "/", restReplayMaster));
                    if (Tools.notEmpty(restReplayMaster) && !fMaster.exists()) {
                        System.err.println("Master file not found: " + fMaster.getCanonicalPath());
                        return;
                    }
                    fMasterPath =  fMaster.getCanonicalPath();
                }
            }

            String testdirResolved = (new File(testdir)).getCanonicalPath();
            System.out.println("RestReplay ::"
                            + "\r\n    testdir: " + testdir
                            + "\r\n    testdir(resolved): " + testdirResolved
                            + "\r\n    control: " + controlFilename
                            + "\r\n    master: " + restReplayMaster
                            + "\r\n    testGroup: " + testGroupID
                            + "\r\n    test: " + testID
                            + "\r\n    env: " + envID
                            + "\r\n    autoDeletePOSTS: " + bAutoDeletePOSTS
                            + (Tools.notEmpty(restReplayMaster)
                            ? ("\r\n    will use master file: " + fMasterPath)
                            : ("\r\n    will use control file: " + f.getCanonicalPath()))
            );

            ResourceManager rootResourceManager = ResourceManager.createRootResourceManager();

            if (Tools.notEmpty(restReplayMaster)) {
                //****************** RUNNING MASTER ******************************************
                Master master = new Master(testdirResolved, reportsDir, rootResourceManager);
                if (Tools.notBlank(selfTestPort)){
                    master.getVars().put("SELFTEST_PORT", selfTestPort);
                }
                master.setEnvID(envID);
                master.readOptionsFromMasterConfigFile(restReplayMaster);
                master.setAutoDeletePOSTS(bAutoDeletePOSTS);
                Dump dumpFromMaster = master.getDump();
                if (Tools.notEmpty(dumpResultsFromCmdLine)) {
                    dumpFromMaster.payloads = bDumpResults;
                }
                master.setDump(dumpFromMaster);
                if (Tools.notEmpty(controlFilename)) {
                    //******* RUN CONTROL, using MASTER
                    System.out.println("INFO: control: " + controlFilename + " will be used within master specified: " + restReplayMaster);
                    master.runMaster(restReplayMaster,
                            false,
                            controlFilename,
                            testGroupID,
                            testID);
                } else {
                    //******** RUN MASTER
                    master.runMaster(restReplayMaster, false); //false, because we already just read the options, and override a few.
                }
            } else {
                //****************** RUNNING CONTROL, NO MASTER ******************************
                RestReplay restReplay = new RestReplay(testdirResolved, reportsDir, rootResourceManager, null);
                restReplay.readDefaultRunOptions();
                Dump dump = Dump.getDumpConfig();
                if (Tools.notEmpty(dumpResultsFromCmdLine)) {
                    dump.payloads = bDumpResults;
                }
                restReplay.setDump(dump);
                if (Tools.notBlank(selfTestPort)){
                    restReplay.getMasterVars().put("SELFTEST_PORT", selfTestPort);
                }

                List<String> reportsList = new ArrayList<String>();
                restReplay.runRestReplayFile(
                        testdirResolved,
                        controlFilename,
                        testGroupID,
                        testID,
                        null,
                        bAutoDeletePOSTS,
                        "",
                        null,
                        reportsList,   // method prints out reportsList if last parameter (masterFilenameInfo) is empty.
                        reportsDir,
                        "",            //no master, so no env in path.
                        "");           //masterFilenameInfo. If blank, stand-alone report with reportsList is printed.
            }
            if (line.hasOption("noexit")){
                pause("RestReplay is now waiting to be profiled. Hit enter when ready to continue.");
            }
        } catch (ParseException exp) {
            System.err.println("Cmd-line parsing failed.  Reason: " + exp.getMessage());
            //System.err.println(usage());
            printHelp(options);
        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (selfTestServer!=null){
                selfTestServer.stopServer();
            }
        }
    }
}
