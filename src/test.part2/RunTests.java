package test.part2;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;

import java.io.*;
import java.net.URISyntaxException;


public class RunTests {
    private static final long TESTSLEEP = 1000; // 1 seconds
    private static final int DEFAULTTOTALPOINTS = 100;
    private static final int DEFAULTFPOINTS = 3;
    private static final boolean DEFAULTCLEARDATA = true;
    private static final String ENDTESTID = "_END_";

    private static final String SCRIPTDIR = "scripts/";
    private static final String LOGDIR = "results/";
    private static final String OUTSUFFIX = ".out";
    private static final String ERRSUFFIX = ".err";
    private static final String GRADESFILE = "grades.txt";

    private static String classpath;

    public static void main(String[] args) throws URISyntaxException {
        classpath = new File(RunTests.class.getClassLoader().getResource("").toURI()).getPath();


        System.out.println("RunTests started.");

        DocumentBuilderFactory dbf = null;
        try {
            dbf = DocumentBuilderFactory.newInstance();
        } catch (FactoryConfigurationError fce) {
            System.err.println(fce);
            System.exit(1);
        }
        dbf.setValidating(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setIgnoringComments(true);

        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            System.err.println(pce);
            System.exit(1);
        }

        Document doc = null;
        try {
            doc = db.parse(new File(classpath + "/test.part2/MASTER.xml"));
        } catch (SAXException se) {
            System.err.println(se);
            System.exit(1);
        } catch (IOException ioe) {
            System.err.println(ioe);
            System.exit(1);
        }

        int totalPoints = DEFAULTTOTALPOINTS;

        Element tests = doc.getDocumentElement();
        if (!tests.getNodeName().equals("tests")) {
            System.err.println("Root element not 'tests'.");
            System.exit(1);
        }
        NamedNodeMap attrs = tests.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);

            if (attr.getNodeName().equals("totalpoints")) {
                totalPoints = Integer.parseInt(attr.getNodeValue());
                System.out.println("Total points set to: " + totalPoints);
            } else {
                System.err.println("Unknown attribute: " + attr.getNodeName());
                System.exit(1);
            }
        }

        int score = totalPoints;
        PrintWriter grades = null;
        try {
            grades = new PrintWriter(new FileWriter(LOGDIR + GRADESFILE,
                    true));    // append
        } catch (IOException e) {
            System.err.println("Cannot open grades file: " + e);
            System.exit(1);
        }
        grades.println("For each test your code failed, we have listed the points deducted and the most likely reason:\n");

        String nextTest = null;
        for (Node test = tests.getFirstChild(); test != null; test = test.getNextSibling()) {
            if (!test.getNodeName().equals("test")) {
                System.err.println("Element not test");
                System.exit(1);
            }

            String id = null;
            boolean clearData = DEFAULTCLEARDATA;
            int fPoints = DEFAULTFPOINTS;
            String fNext = null;

            attrs = test.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);

                if (attr.getNodeName().equals("id")) {
                    id = attr.getNodeValue();
                    //System.out.println("ID set to: " + id);
                } else if (attr.getNodeName().equals("cleardata")) {
                    clearData = attr.getNodeValue().startsWith("t");
                    //System.out.println("clearData set to: " + clearData);
                } else if (attr.getNodeName().equals("fpoints")) {
                    fPoints = Integer.parseInt(attr.getNodeValue());
                    //System.out.println("fPoints set to: " + fPoints);
                } else if (attr.getNodeName().equals("fnext")) {
                    fNext = attr.getNodeValue();
                    //System.out.println("fNext set to: " + fNext);
                } else {
                    System.err.println("Unknown attribute: " + attr.getNodeName());
                    System.exit(1);
                }
            }

            if (id == null) {
                System.err.println("No ID given");
                System.exit(1);
            }
            if (fPoints < 0) {
                System.err.println("fpoints has to be >=0");
                System.exit(1);
            }
            if (nextTest != null && !id.equals(nextTest)) {
                System.out.println("Skipping test: " + id);
                continue;
            }

            String fmsg = test.getFirstChild().getNodeValue().trim();
            //System.out.println("fmsg is: " + fmsg);

            String command;
            String opt;
            String os = System.getProperty("os.name").toLowerCase();
            if(os.contains("windows")) {
                command = "CMD.exe";
                opt = "/C";
            }
            else {
                command = "sh";
                opt = "-c";
            }

            if (clearData) {
                System.out.println("Clearing data");

                delDir(new File("data"));
//			System.exit(1);



		/*
        if (!(new File("data").mkdir())) {
		    System.err.println("mkdir data not successful");
		    System.exit(1);
		}
		*/
            }

            System.out.println("Launching test " + id);
            Process proc = null;
            try {
                proc = Runtime.getRuntime().exec(new String[]{
                        command,
                        opt,
                        "java -classpath " + classpath + " -DrmiPort=" +
                                System.getProperty("rmiPort") +
                                " -Djava.security.policy=" + classpath + "/../conf/security-policy transaction.Client <" +
                                SCRIPTDIR + id +
                                " >" + LOGDIR + id + OUTSUFFIX +
                                " 2>" + LOGDIR + id + ERRSUFFIX});
            } catch (IOException e) {
                System.err.println("Cannot launch Client: " + e);
                System.exit(1);
            }

            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                System.err.println("WaitFor interrupted.");
                System.exit(1);
            }

            int exitVal = proc.exitValue();
            if (exitVal == 0) {
                System.out.println("Test " + id + " passed.");
                nextTest = null;
            } else if (exitVal == 2) {
                System.out.println("Test " + id + " failed.");
                grades.println("(" + id + ")\t-" + fPoints);
                grades.println(fmsg + "\n");
                score -= fPoints;
                if (ENDTESTID.equals(fNext)) {
                    nextTest = null;
                    break;
                }
                nextTest = fNext;
            } else {
                System.err.println("Test " + id + " errored (" + exitVal + ")");
                System.exit(1);
            }

            try {
                Thread.sleep(TESTSLEEP);
            } catch (InterruptedException e) {
                System.err.println("Sleep interrupted.");
                System.exit(1);
            }
        }
        if (nextTest != null) {
            System.err.println("Expected test " + nextTest + " not found");
            System.exit(1);
        }

        if (score < 0) {
            score = 0;
        }
        System.out.println("Your final score is " +
                score + "/" + totalPoints);
        grades.println("============================================");
        grades.println("Your final score is " +
                score + "/" + totalPoints);
        grades.close();
        System.exit(0);
    }

    public static void delDir(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File zFiles[] = file.listFiles();
            for (File file2 : zFiles) {
                delDir(file2);
            }
            file.delete();
        } else {
            file.delete();
        }
    }
}
