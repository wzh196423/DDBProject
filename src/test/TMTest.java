package test;

import org.junit.*;
import transaction.*;

import java.io.File;
import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;


/**
 * Created by 14302 on 2019/7/27.
 */
public class TMTest {
    private TransactionManager tm;
    private ResourceManager rmflights;
    private ResourceManager rmcars;
    private ResourceManager rmrooms;
    private ResourceManager rmcustomers;



    @BeforeClass
    public void init() throws RemoteException {
        System.setProperty("java.security.policy", "C:\\Users\\wch\\Desktop\\课程\\DDBProject\\src\\transaction\\security-policy");
        tm = TransactionManagerImpl.init();
        rmcars = ResourceManagerImpl.init(ResourceManager.RMINameCars);
        rmcustomers = ResourceManagerImpl.init(ResourceManager.RMINameCustomers);
        rmflights = ResourceManagerImpl.init(ResourceManager.RMINameFlights);
        rmrooms = ResourceManagerImpl.init(ResourceManager.RMINameRooms);
    }

    @Before
    public void startUp() throws RemoteException {
        tm.setDieTime("no");
        rmflights.setDieTime("NoDie");
        rmcars.setDieTime("NoDie");
        rmrooms.setDieTime("NoDie");
        rmcustomers.setDieTime("NoDie");
    }

    @After
    public void tearDown() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            return;
        }

        if (dataDir.isDirectory()) {
            File[] files = dataDir.listFiles();
            for (File f : files) {
                f.delete();
            }
        }
        dataDir.delete();
    }

    @Test
    public void testStart() {
        try {
            int xid1 = tm.start();
        }
        catch (Exception e) {
            Assert.fail();
        }
    }




}
