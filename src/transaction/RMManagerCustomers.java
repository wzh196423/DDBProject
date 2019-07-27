/*
 * Created on 2005-5-29
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package transaction;

import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RMManagerCustomers {
	static Registry _rmiRegistry = null;

	public static void main(String[] args) {
		String rmiName = ResourceManager.RMINameCustomers;

		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("conf/ddb.conf"));
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}

		String rmiPort = prop.getProperty("rm." + rmiName + ".port");
		try {
			LocateRegistry.createRegistry(Integer.parseInt(rmiPort));
		}
		catch (Exception e) {
			System.out.println("Port has registered.");
		}
		if (rmiPort == null)
		{
			rmiPort = "";
		}
		else if (!rmiPort.equals("")) {
			rmiPort = "//:" + rmiPort + "/";
		}

		try {
			ResourceManager obj = new ResourceManagerImpl(rmiName);
			Naming.rebind(rmiPort + rmiName, obj);
			System.out.println(rmiName + " bound");
		} catch (Exception e) {
			System.err.println(rmiName + " not bound:" + e);
			System.exit(1);
		}

	}
}
