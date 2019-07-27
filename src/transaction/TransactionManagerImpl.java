package transaction;

import java.rmi.*;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

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



    public static void main(String args[]) {
        System.setSecurityManager(new RMISecurityManager());

        String rmiPort = System.getProperty("rmiPort");
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        try {
            TransactionManagerImpl obj = new TransactionManagerImpl();
            Naming.rebind(rmiPort + TransactionManager.RMIName, obj);
            System.out.println("TM bound");
        } catch (Exception e) {
            System.err.println("TM not bound:" + e);
            System.exit(1);
        }
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
        int xid = ++count;
        List<ResourceManager> list = new ArrayList<>();
        hold_list.put(xid, list);
        return xid;
    }

    @Override
    public boolean commit(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!hold_list.containsKey(xid))
            return false;
        List<ResourceManager> list = hold_list.get(xid);
        ResourceManager rm;
        for (int i = 0 ; i < list.size(); i++){
            rm = list.get(i);
            rm.commit(xid);
            System.out.println(rm.getID() + "committed!");
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
            System.out.println(rm.getID() + "aborted!");
        }
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

    public void setDieTime(String time){
        this.dieTime = time;
    }

}
