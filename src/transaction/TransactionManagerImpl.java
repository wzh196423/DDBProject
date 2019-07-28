package transaction;

import java.io.FileInputStream;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Transaction Manager for the Distributed Travel Reservation System.
 * <p>
 * Description: toy implementation of the TM
 */

public class TransactionManagerImpl
        extends java.rmi.server.UnicastRemoteObject
        implements TransactionManager {

    // 存储对于4个RM的引用
//    ResourceManager carRM = null;
//    ResourceManager customerRM = null;
//    ResourceManager flightRM = null;
//    ResourceManager hotelRM = null;
    private Hashtable<Integer, List<ResourceManager>> hold_list;
    private int count;
    private String dieTime;

    private AtomicBoolean wait = new AtomicBoolean(false);



    public static void main(String args[]) throws RemoteException {
        System.setSecurityManager(new RMISecurityManager());

        Properties prop = new Properties();
        try
        {
            prop.load(new FileInputStream("conf/ddb.conf"));
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
            return;
        }
        String rmiPort = prop.getProperty("tm.port");
        try {
            LocateRegistry.createRegistry(Integer.parseInt(rmiPort));
        }
        catch (Exception e) {
            System.out.println("Port has registered.");
        }
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        try {
            TransactionManagerImpl obj = new TransactionManagerImpl();
            Naming.bind(rmiPort + TransactionManager.RMIName, obj);
            System.out.println("TM bound");
        } catch (Exception e) {
            System.err.println("TM not bound:" + e);
            System.exit(1);
        }
    }

    public static TransactionManager init() throws RemoteException {
        System.setSecurityManager(new RMISecurityManager());

        Properties prop = new Properties();
        try
        {
            prop.load(new FileInputStream("conf/ddb.conf"));
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
            return null;
        }
        String rmiPort = prop.getProperty("tm.port");
        try {
            LocateRegistry.createRegistry(Integer.parseInt(rmiPort));
        }
        catch (Exception e) {
            System.out.println("Port has registered.");
        }
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        TransactionManagerImpl obj = null;
        try {
            obj = new TransactionManagerImpl();
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

    public void enlist(int xid, ResourceManager rm) throws RemoteException {
        if (!hold_list.containsKey(xid))
            return;
        List<ResourceManager> list = hold_list.get(xid);
        list.add(rm);
        hold_list.put(xid, list);

    }

    @Override
    public int start() throws RemoteException {
        synchronized (wait) {
            while (wait.get()) {
                try {
                    wait.wait();
                } catch (InterruptedException e) {
                    return -1;
                }

            }

            int xid = ++count;
            List<ResourceManager> list = new ArrayList<>();
            hold_list.put(xid, list);

            wait.set(false);
            wait.notifyAll();
            return xid;
        }
    }

    @Override
    public boolean commit(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!hold_list.containsKey(xid))
            return false;
        List<ResourceManager> list = hold_list.get(xid);
        List<ResourceManager> preparedList = new ArrayList<>(list.size());
        ResourceManager rm;
        for (int i = 0 ; i < list.size(); i++){
            rm = list.get(i);
            try {
                if (rm.prepare(xid)) {
                    preparedList.add(rm);
                }
            }
            catch (Exception e) {
                System.out.println(rm.getID() + " has died!");
            }
        }
        if (preparedList.size() == list.size()) {
            for (int i = 0; i < list.size(); i++) {
                rm = list.get(i);
                rm.commit(xid);
                System.out.println(rm.getID() + " committed!");
            }
            hold_list.remove(xid);
        }
        else {
            for (int i = 0; i < preparedList.size(); i++) {
                rm = preparedList.get(i);
                rm.abort(xid);
                System.out.println(rm.getID() + " aborted!");
            }
            hold_list.remove(xid);
            throw new TransactionAbortedException(xid, "Transaction aborted!");
        }
        return true;
    }

    @Override
    public void abort(int xid) throws RemoteException, InvalidTransactionException {
        if (!hold_list.containsKey(xid)){
            return;
        }
        List<ResourceManager> list = hold_list.get(xid);
        ResourceManager rm;
        for (int i = 0 ; i < list.size(); i++){
            rm = list.get(i);
            rm.abort(xid);
            System.out.println(rm.getID() + " aborted!");
        }

        hold_list.remove(xid);
    }

    public TransactionManagerImpl() throws RemoteException {
        hold_list = new Hashtable<>();
        count = 0;
        dieTime = "no";
    }

    public boolean dieNow()
            throws RemoteException {
        System.exit(1);
        return true; // We won't ever get here since we exited above;
        // but we still need it to please the compiler.
    }

    public void setDieTime(String time) throws RemoteException{
        this.dieTime = time;
    }

}
