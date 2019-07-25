package transaction;

import java.rmi.*;

/** 
 * Interface for the Transaction Manager of the Distributed Travel
 * Reservation System.
 * <p>
 * Unlike WorkflowController.java, you are supposed to make changes
 * to this file.
 */

public interface TransactionManager extends Remote {

    public boolean dieNow()
	throws RemoteException;

    public void ping() throws RemoteException;
    
	public void enlist(int xid, ResourceManager rm) throws RemoteException;

	// 新添的接口，被workflow controller调用的三个方法
	public int start() throws RemoteException;

	public boolean commit(int xid)
            throws RemoteException, TransactionAbortedException, TransactionManagerUnaccessibleException, InvalidTransactionException;

	public void abort(int xid)
            throws RemoteException, TransactionAbortedException, TransactionManagerUnaccessibleException, InvalidTransactionException;

	
    /** The RMI name a TransactionManager binds to. */
    public static final String RMIName = "TM";
}
