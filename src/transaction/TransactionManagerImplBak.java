package transaction;

import java.io.*;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Transaction Manager for the Distributed Travel Reservation System.
 * <p>
 * Description: toy implementation of the TM
 */

public class TransactionManagerImplBak
        extends java.rmi.server.UnicastRemoteObject
        implements TransactionManager {

    // 存储对于4个RM的引用
//    ResourceManager carRM = null;
//    ResourceManager customerRM = null;
//    ResourceManager flightRM = null;
//    ResourceManager hotelRM = null;
    private Hashtable<Integer, Set<ResourceManager>> hold_list;
    private Integer count;
    private String dieTime;
    private static String classpath;

    private static String RECORD_DIR = "data";
    private static String RECORD_FILE = RECORD_DIR + "/transaction.record";

    public static final String STARTED = "started";
    public static final String COMMITTED = "committed";
    public static final String ABORTED = "aborted";

    private Hashtable<Integer, String> statusTable = new Hashtable<>();


    private AtomicBoolean wait = new AtomicBoolean(false);
    private AtomicBoolean recordLock = new AtomicBoolean(false);


    private Set<Integer> waitingToAbort = new HashSet<>();

    public static void main(String args[]) throws RemoteException, URISyntaxException {
        classpath = new File(TransactionManagerImplBak.class.getClassLoader().getResource("").toURI()).getPath();

        System.setSecurityManager(new RMISecurityManager());

        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(classpath + "/../conf/ddb.conf"));
        } catch (Exception e1) {
            e1.printStackTrace();
            return;
        }
        String rmiPort = prop.getProperty("tm.port");
        try {
            LocateRegistry.createRegistry(Integer.parseInt(rmiPort));
        } catch (Exception e) {
            System.out.println("Port has registered.");
        }
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        try {
            TransactionManagerImplBak obj = new TransactionManagerImplBak();
            Naming.bind(rmiPort + TransactionManager.RMIName, obj);
            System.out.println("TM bound");
        } catch (Exception e) {
            System.err.println("TM not bound:" + e);
            System.exit(1);
        }
    }

    public static TransactionManager init() throws RemoteException, URISyntaxException {
        classpath = new File(TransactionManagerImplBak.class.getClassLoader().getResource("").toURI()).getPath();

        System.setSecurityManager(new RMISecurityManager());

        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(classpath + "/../conf/ddb.conf"));
        } catch (Exception e1) {
            e1.printStackTrace();
            return null;
        }
        String rmiPort = prop.getProperty("tm.port");
        try {
            LocateRegistry.createRegistry(Integer.parseInt(rmiPort));
        } catch (Exception e) {
            System.out.println("Port has registered.");
        }
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        TransactionManagerImplBak obj = null;
        try {
            obj = new TransactionManagerImplBak();
            Naming.bind(rmiPort + TransactionManager.RMIName, obj);
            System.out.println("TM bound");
        } catch (Exception e) {
            System.err.println("TM not bound:" + e);
            System.exit(1);
        }

        return obj;
    }

    public void ping() throws RemoteException {
    }

    public void recordStatus(int xid, String status) throws RemoteException {
        synchronized (statusTable) {
            statusTable.put(xid, status);
            storeStatusLogs(statusTable);
        }
    }

    public String readStatus(int xid) throws RemoteException {
        if(statusTable.contains(xid)) {
            return statusTable.get(xid);
        }
        return null;
    }

    public void enlist(int xid, ResourceManager rm) throws RemoteException {

        if (!hold_list.containsKey(xid)) {
            // means the transaction(id=xid) has been committed
//            String status = readStatus(xid);
//            System.out.println("xid=" + xid + ", status=" + status);
//            try {
//                if (COMMITTED.equals(status))
//                    rm.commit(xid);
//                else if (ABORTED.equals(status))
//                    rm.abort(xid);
//            } catch (InvalidTransactionException e) {
//                e.printStackTrace();
//                throw new RemoteException("Enlist fail!");
//            }
            return;
        }
        synchronized (hold_list) {
            System.out.println("Enlist xid = " + xid + " " + rm.getID());
            Set<ResourceManager> list = hold_list.get(xid);
            list.add(rm);
            hold_list.put(xid, list);
        }
    }

    @Override
    public int start() throws RemoteException {
        int xid = -1;
        synchronized (hold_list) {
            while (wait.get()) {
                try {
                    wait.wait();
                } catch (InterruptedException e) {
                    return -1;
                }

            }

            xid = ++count;
            Set<ResourceManager> list = new HashSet<>();
            hold_list.put(xid, list);

            wait.set(false);

        }
        if (xid > 0)
            recordStatus(xid, STARTED);

        return xid;
    }

    @Override
    public boolean commit(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!hold_list.containsKey(xid))
            return false;

        Set<ResourceManager> list = hold_list.get(xid);
        if (waitingToAbort.contains(xid)) {
            synchronized (hold_list) {
                hold_list.remove(xid);
            }
            waitingToAbort.remove(xid);
            recordStatus(xid, ABORTED);
            throw new TransactionAbortedException(xid, "Transaction aborted!");
        }
        Set<ResourceManager> preparedList = new HashSet<>(list.size());
        for (ResourceManager rm: list) {
            try {
                if (rm.prepare(xid)) {
                    preparedList.add(rm);
                }
            } catch (Exception e) {
//                e.printStackTrace();
                System.out.println("Some rm has died!");
            }
        }
        if (dieTime.equals("BeforeCommit"))
            dieNow();
        if (preparedList.size() == list.size()) {
            for (ResourceManager rm : list) {
                try {
                    rm.commit(xid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            synchronized (hold_list) {
                hold_list.remove(xid);
            }
            recordStatus(xid, COMMITTED);
            System.out.println("All rm has committed successfully!");
            if (dieTime.equals("AfterCommit"))
                dieNow();
        } else {
            for (ResourceManager rm : preparedList) {
                rm.abort(xid);
                System.out.println("Some rm has aborted!");
            }
            synchronized (hold_list) {
                hold_list.remove(xid);
            }
            recordStatus(xid, ABORTED);
            System.out.println("All rm has aborted successfully since some rm has aborted before!");
            throw new TransactionAbortedException(xid, "Transaction aborted!");
        }
        return true;
    }

    @Override
    public void abort(int xid) throws RemoteException, InvalidTransactionException {
        if (!hold_list.containsKey(xid)) {
            return;
        }
        Set<ResourceManager> list = hold_list.get(xid);
        for (ResourceManager rm : list) {
            rm.abort(xid);
            System.out.println(rm.getID() + " aborted!");
        }
        synchronized (hold_list) {
            hold_list.remove(xid);

        }

        recordStatus(xid, ABORTED);
    }

    public void recover() throws RemoteException{
        Hashtable<Integer, String> tmpTable = loadStatusLogs();
        if(tmpTable != null) {
            statusTable = tmpTable;
        }
        HashSet<Integer> t_xids = new HashSet<>(100);
        for (Map.Entry<Integer, String> entry: statusTable.entrySet()) {
            int xid = entry.getKey();
            if(xid > count) {
                count = xid;
            }
            String status = entry.getValue();
            if(STARTED.equals(status)) {
                t_xids.add(xid);
            }
            else if(COMMITTED.equals(status) || ABORTED.equals(status)) {
                t_xids.remove(xid);
            }
        }

        synchronized (hold_list) {
            for(int xid: t_xids) {
                hold_list.put(xid, new HashSet<ResourceManager>());
            }
        }

        synchronized (waitingToAbort) {
            for(int xid: t_xids) {
                System.out.println("Wait to abort: " + xid);
                waitingToAbort.add(xid);
            }
        }

    }

    public TransactionManagerImplBak() throws RemoteException {
        hold_list = new Hashtable<>();
        count = 0;
        dieTime = "no";

        File recordDir = new File("data");
        if (!recordDir.exists()) {
            recordDir.mkdirs();
        }



        recover();
    }

    public boolean dieNow()
            throws RemoteException {
        System.exit(1);
        return true; // We won't ever get here since we exited above;
        // but we still need it to please the compiler.
    }

    public void setDieTime(String time) throws RemoteException {
        this.dieTime = time;
    }

    protected Hashtable<Integer, String> loadStatusLogs()
    {
        File xidLog = new File("data/status.log");
        ObjectInputStream oin = null;
        try
        {
            oin = new ObjectInputStream(new FileInputStream(xidLog));
            return (Hashtable<Integer, String>) oin.readObject();
        }
        catch (Exception e)
        {
            return null;
        }
        finally
        {
            try
            {
                if (oin != null)
                    oin.close();
            }
            catch (IOException e1)
            {
            }
        }
    }

    protected boolean storeStatusLogs(Hashtable<Integer, String> statusTable)
    {
        File xidLog = new File("data/status.log");
        xidLog.getParentFile().mkdirs();
        xidLog.getParentFile().mkdirs();
        ObjectOutputStream oout = null;
        try
        {
            oout = new ObjectOutputStream(new FileOutputStream(xidLog));
            oout.writeObject(statusTable);
            oout.flush();
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
        finally
        {
            try
            {
                if (oout != null)
                    oout.close();
            }
            catch (IOException e1)
            {
            }
        }
    }

}
