package test;

import transaction.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by 14302 on 2019/7/28.
 */
public class LockDead {
    private static final long TESTTIMEOUT = 180000; // 3 minutes
    private static final long LAUNCHSLEEP = 1000; // 5 seconds
    private static final long BCNEXTOPDELAY = 1000; // 1 second
    private static final long BCFINISHDELAY = 500; // 1/2 second

    private static final String DELAYMARKER = "_DLMKR_";

    private static final String LOGDIR = "results/";
    private static final String LOGSUFFIX = ".log";

    private static String rmiPort = null;
    private static WorkflowController wc = null;

    private static AtomicBoolean lock = new AtomicBoolean(false);

    private static String classpath = null;

    static class TransactionThread extends Thread {
        int xid;

        public TransactionThread() {
        }

        public void run() {
            synchronized (lock) {
                while (!lock.get()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Dead lock fail!");
                        dieAll();
                    }
                }

                try {
                    xid = wc.start();
//                    System.out.println("Thread: " + xid);
                    if (!wc.reserveFlight(xid, "John", "347")) {
                        System.out.println("Dead lock fail!");
                        dieAll();
                    }

                    if (!wc.addRooms(xid, "Stanford", 200, 300)) {
                        System.out.println("Dead lock fail!");
                        dieAll();
                    }
                } catch (Exception e) {
                    System.out.println("Dead lock fail!");
                    dieAll();
                }


                lock.set(false);
                lock.notifyAll();

                while (!lock.get()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Dead lock fail!");
                        dieAll();
                    }
                }

                try {
                    wc.queryCarsPrice(xid, "SFO");

                    dieAll();
                } catch (TransactionAbortedException e) {
                    System.out.println("Catch Exception!");
                    lock.set(false);
                    lock.notifyAll();

                } catch (Exception e) {
                    System.out.println("Dead lock fail!");
                    dieAll();

                }
            }


        }
    }

    public static void main(String args[]) throws URISyntaxException, InterruptedException {
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

        TransactionThread thread = new TransactionThread();
        thread.start();

        int xid = 0;

        synchronized (lock) {

            try {
                xid = wc.start();
//                System.out.println(xid);
                if (!wc.addFlight(xid, "347", 100, 310)) {
                    System.out.println("Add flight fail!");
                    dieAll();
                }
                if (!wc.addRooms(xid, "Stanford", 200, 150)) {
                    System.out.println("Add rooms fail!");
                    dieAll();
                }
                if (!wc.addCars(xid, "SFO", 300, 30)) {
                    System.out.println("Add cars fail!");
                    dieAll();
                }
                if (!wc.newCustomer(xid, "John")) {
                    System.out.println("Add customers fail!");
                    dieAll();
                }

                if (!wc.commit(xid)) {
                    System.out.println("Commit fail!");
                    dieAll();
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Add and commit fail!");
                dieAll();
            }

            try {
                xid = wc.start();
//                System.out.println(xid);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Add and commit fail!");
                dieAll();
            }


            lock.set(true);
            lock.notifyAll();

            while (lock.get()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    System.out.println("Dead lock fail!");
                    dieAll();
                }
            }

            try {
                if (!wc.addCars(xid, "SFO", 300, 60)) {
                    System.out.println("Add and commit fail!");
                    dieAll();
                }
            } catch (Exception e) {
                System.out.println("Add and commit fail!");
                dieAll();
            }


            lock.set(true);
            lock.notifyAll();

            while (lock.get()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    System.out.println("Dead lock fail!");
                    dieAll();
                }
            }

            try {
                if (!wc.commit(xid)) {
                    System.out.println("Add and commit fail!");
                    dieAll();
                }
            } catch (Exception e) {
                System.out.println("Add and commit fail!");
                dieAll();
            }

            try {

                xid = wc.start();
//                System.out.println(xid);

                if (wc.queryFlight(xid, "347") != 100) {
                    System.out.println("Query flight fail!");
                    dieAll();
                }
                if (wc.queryFlightPrice(xid, "347") != 310) {
                    System.out.println("Query flights price fail!");
                    dieAll();
                }
                if (wc.queryRooms(xid, "Stanford") != 200) {
                    System.out.println("Query rooms fail!");
                    dieAll();
                }
                if (wc.queryRoomsPrice(xid, "Stanford") != 150) {
                    System.out.println("Query rooms price fail!");
                    dieAll();
                }

//                System.out.println(wc.queryCars(xid, "SFO"));
//                System.out.println(wc.queryCarsPrice(xid, "SFO"));
                if (wc.queryCars(xid, "SFO") != 600) {
                    System.out.println("Query cars fail!");
                    dieAll();
                }
                if (wc.queryCarsPrice(xid, "SFO") != 60) {
                    System.out.println("Query cars price fail!");
                    dieAll();
                }

                if (wc.queryCustomerBill(xid, "John") != 0) {
                    System.out.println("Query customer bill fail!");
                    dieAll();
                }
            } catch (Exception e) {
                System.out.println("Query customer bill fail!");
                dieAll();
            }
        }

        System.out.println("Dead lock pass!");
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
        if (os.contains("windows")) {
            command = "CMD.exe";
            opt = "/C";
        } else {
            command = "sh";
            opt = "-c";
        }

        try {
            Runtime.getRuntime().exec(new String[]{
                    command,
                    opt,
                    "java -classpath " + classpath +
                            " -DrmiName=" + rmiName +
                            " -Djava.security.policy=" + classpath + "/transaction/security-policy transaction." + className +
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


        if (rmiName.equals(WorkflowController.RMIName)) {
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
