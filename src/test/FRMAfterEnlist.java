package test;

import transaction.Client;
import transaction.ResourceManager;
import transaction.TransactionManager;
import transaction.WorkflowController;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Created by 14302 on 2019/7/28.
 */
public class FRMAfterEnlist {
    private static final long TESTTIMEOUT = 180000; // 3 minutes
    private static final long LAUNCHSLEEP = 1000; // 5 seconds
    private static final long BCNEXTOPDELAY = 1000; // 1 second
    private static final long BCFINISHDELAY = 500; // 1/2 second

    private static final String DELAYMARKER = "_DLMKR_";

    private static final String LOGDIR = "results/";
    private static final String LOGSUFFIX = ".log";

    private static String rmiPort = null;
    private static WorkflowController wc = null;
    private static String currentLine = null;
    private static BufferedReader scriptReader =
            new BufferedReader(new InputStreamReader(System.in));

    private static String classpath = null;

    public static void main(String args[]) throws URISyntaxException {
        classpath = new File(Add.class.getClassLoader().getResource("").toURI()).getPath();

        File dataDir = new File("data");
        delDir(dataDir);
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(classpath + "/../conf/ddb.conf"));
        } catch (Exception e1) {
            e1.printStackTrace();
            return;
        }
        rmiPort = prop.getProperty("wc.port");

        launchAll();

        int xid = 0;
        try {
            xid = wc.start();
            if(!wc.addFlight(xid, "347", 100, 310)) {
                System.out.println("Add flight fail!");
                dieAll();
            }
            if(!wc.addRooms(xid, "Stanford", 200, 150)) {
                System.out.println("Add rooms fail!");
                dieAll();
            }
            if(!wc.addCars(xid, "SFO", 300, 30)) {
                System.out.println("Add cars fail!");
                dieAll();
            }
            if(!wc.newCustomer(xid, "John")) {
                System.out.println("Add customers fail!");
                dieAll();
            }

            if(!wc.commit(xid)) {
                System.out.println("Commit fail!");
                dieAll();
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Add and commit fail!");
            dieAll();
        }

        try {
            xid = wc.start();
            if(!wc.dieRMAfterEnlist("RMRooms")) {
                System.out.println("Die RMRooms fail!");
                dieAll();
            }
            if(!wc.addFlight(xid, "347", 100, 620)) {
                System.out.println("Add flights fail!");
                dieAll();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Add  fail!");
            dieAll();
        }

        try {
            wc.reserveRoom(xid, "John", "Stanford");
            System.out.println("Reverse fail!");
            dieAll();
        }
        catch (Exception e) {

        }

        try {
            launch(ResourceManager.RMINameRooms, "ResourceManagerImpl");
            if(!wc.reconnect()) {
                System.out.println("Launch RMRooms fail!");
                dieAll();
            }
        }catch (Exception e) {
            System.out.println("Launch RMRooms fail!");
            dieAll();
        }

        System.out.println("Add and commit pass!");
        dieAll();

        delDir(dataDir);

    }


    private static void launchAll() {
        String[] rmiNames = new String[]{TransactionManager.RMIName,
                ResourceManager.RMINameFlights,
                ResourceManager.RMINameRooms,
                ResourceManager.RMINameCars,
                ResourceManager.RMINameCustomers,
                WorkflowController.RMIName};
        String[] classNames = new String[]{"TransactionManagerImpl",
                "ResourceManagerImpl",
                "ResourceManagerImpl",
                "ResourceManagerImpl",
                "ResourceManagerImpl",
                "WorkflowControllerImpl"
        };

        for (int i = 0; i < rmiNames.length; i++) {
            launch(rmiNames[i], classNames[i]);
        }
    }

    private static void launch(String rmiName, String className) {
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

        try {
            Runtime.getRuntime().exec(new String[]{
                    command,
                    opt,
                    "java -classpath " + classpath +
                            " -DrmiName=" + rmiName +
                            " -Djava.security.policy="+ classpath+"/transaction/security-policy transaction." + className +
                            " >>" + LOGDIR + rmiName + LOGSUFFIX + " 2>&1"
            });
        } catch (IOException e) {
            System.err.println("Cannot launch " + rmiName + ": " + e);
            dieAll();
        }
        System.out.println(rmiName + " launched");

        try {
            Thread.sleep(LAUNCHSLEEP);
        } catch (InterruptedException e) {
            System.err.println("Sleep interrupted.");
            System.exit(1);
        }


        if(rmiName.equals(WorkflowController.RMIName)) {
            try {
                wc = (WorkflowController) Naming.lookup("//:" + rmiPort + "/" + WorkflowController.RMIName);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Cannot bind to " + WorkflowController.RMIName + ": " + e);
                dieAll();
            }
        }
    }


    private static void dieAll() {
        try {
            wc.dieNow("ALL");
        } catch (Exception e) {

        }
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
