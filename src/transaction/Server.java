package transaction;

import java.io.File;
import java.net.URISyntaxException;
import java.rmi.RemoteException;

/**
 * Created by 14302 on 2019/7/27.
 */
public class Server {
    public static void main(String[] args) throws RemoteException, URISyntaxException {
        System.setProperty("java.security.policy", "/Users/wangziheng/git/DDBProject/src/transaction/security-policy");
        TransactionManagerImpl.init();
        ResourceManagerImpl.init(ResourceManager.RMINameCars);
        ResourceManagerImpl.init(ResourceManager.RMINameCustomers);
        ResourceManagerImpl.init(ResourceManager.RMINameFlights);
        ResourceManagerImpl.init(ResourceManager.RMINameRooms);
        WorkflowControllerImpl.init();
    }
}
