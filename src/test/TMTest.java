package test;

import org.jmock.Expectations;
import org.junit.*;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.Mockery;

import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.List;

import transaction.*;



/**
 * Created by 14302 on 2019/7/27.
 */
public class TMTest {
    private static TransactionManager tm;
    private ResourceManager rmflights;
    private ResourceManager rmcars;
    private ResourceManager rmrooms;
    private ResourceManager rmcustomers;

    private Mockery context = new JUnit4Mockery();

    @BeforeClass
    public static void init() throws RemoteException {
        System.setProperty("java.security.policy", "C:\\Users\\wch\\Desktop\\课程\\DDBProject\\src\\transaction\\security-policy");
        tm = TransactionManagerImpl.init();
    }


    @Before
    public void startUp() throws RemoteException {
        rmcars = context.mock(ResourceManager.class, "rmcars");
        rmcustomers = context.mock(ResourceManager.class, "rmcustomers");
        rmflights = context.mock(ResourceManager.class, "rmflights");
        rmrooms = context.mock(ResourceManager.class, "rmrooms");
    }

    @Test
    public void testStart() {
        try {
            int xid1 = tm.start();
            int xid2 = tm.start();
            Assert.assertTrue(xid1 > 0);
            Assert.assertTrue(xid2 > 0);
            Assert.assertNotEquals(xid1, xid2);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testEnlist() throws NoSuchFieldException, IllegalAccessException {
        Field field = tm.getClass().getDeclaredField("hold_list");// 获取PrivateClass所有属性
        field.setAccessible(true);

        Hashtable<Integer, List<ResourceManager>> hold_list = null;

        try {
            int xid = tm.start();

            tm.enlist(xid, rmflights);
            hold_list = (Hashtable<Integer, List<ResourceManager>>) field.get(tm);
            Assert.assertEquals(1, hold_list.get(xid).size());
            Assert.assertTrue(hold_list.get(xid).get(0) == rmflights);

            tm.enlist(xid, rmcars);
            hold_list = (Hashtable<Integer, List<ResourceManager>>) field.get(tm);
            Assert.assertEquals(2, hold_list.get(xid).size());
            Assert.assertTrue(hold_list.get(xid).get(1) == rmcars);

            tm.enlist(xid, rmrooms);
            hold_list = (Hashtable<Integer, List<ResourceManager>>) field.get(tm);
            Assert.assertEquals(3, hold_list.get(xid).size());
            Assert.assertTrue(hold_list.get(xid).get(2) == rmrooms);

            tm.enlist(xid, rmcustomers);
            hold_list = (Hashtable<Integer, List<ResourceManager>>) field.get(tm);
            Assert.assertEquals(4, hold_list.get(xid).size());
            Assert.assertTrue(hold_list.get(xid).get(3) == rmcustomers);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }

    }

    @Test
    public void testCommitSuccessful() throws RemoteException, InvalidTransactionException, NoSuchFieldException, IllegalAccessException {

        int xid = tm.start();

        context.checking(new Expectations() {
            {
                exactly(1).of(rmflights).prepare(xid);
                will(returnValue(true));

                exactly(1).of(rmcars).prepare(xid);
                will(returnValue(true));

                exactly(1).of(rmrooms).prepare(xid);
                will(returnValue(true));

                exactly(1).of(rmcustomers).prepare(xid);
                will(returnValue(true));

                exactly(1).of(rmflights).commit(xid);

                exactly(1).of(rmflights).getID();
                will(returnValue(ResourceManager.RMINameFlights));

                exactly(1).of(rmcars).commit(xid);

                exactly(1).of(rmcars).getID();
                will(returnValue(ResourceManager.RMINameCars));

                exactly(1).of(rmrooms).commit(xid);

                exactly(1).of(rmrooms).getID();
                will(returnValue(ResourceManager.RMINameRooms));

                exactly(1).of(rmcustomers).commit(xid);

                exactly(1).of(rmcustomers).getID();
                will(returnValue(ResourceManager.RMINameCustomers));

            }
        });


        tm.enlist(xid, rmflights);
        tm.enlist(xid, rmcars);
        tm.enlist(xid, rmrooms);
        tm.enlist(xid, rmcustomers);

        try {
            tm.commit(xid);
        } catch (TransactionAbortedException e) {
            Assert.fail();
        }

        Field field = tm.getClass().getDeclaredField("hold_list");// 获取PrivateClass所有属性
        field.setAccessible(true);

        Hashtable<Integer, List<ResourceManager>> hold_list = (Hashtable<Integer, List<ResourceManager>>) field.get(tm);

        Assert.assertFalse(hold_list.containsKey(xid));

    }

    @Test
    public void testCommitFailed() throws RemoteException, InvalidTransactionException, NoSuchFieldException, IllegalAccessException {
        int xid = tm.start();
        System.out.println(xid);
        context.checking(new Expectations() {
            {
                exactly(1).of(rmflights).prepare(xid);
                will(returnValue(true));

                exactly(1).of(rmcars).prepare(xid);
                will(returnValue(false));

                exactly(1).of(rmrooms).prepare(xid);
                will(returnValue(true));

                exactly(1).of(rmcustomers).prepare(xid);
                will(returnValue(true));

                exactly(1).of(rmflights).abort(xid);

                exactly(1).of(rmflights).getID();
                will(returnValue(ResourceManager.RMINameFlights));


                exactly(1).of(rmrooms).abort(xid);

                exactly(1).of(rmrooms).getID();
                will(returnValue(ResourceManager.RMINameRooms));

                exactly(1).of(rmcustomers).abort(xid);

                exactly(1).of(rmcustomers).getID();
                will(returnValue(ResourceManager.RMINameCustomers));
            }
        });


        tm.enlist(xid, rmflights);
        tm.enlist(xid, rmcars);
        tm.enlist(xid, rmrooms);
        tm.enlist(xid, rmcustomers);

        try {
            tm.commit(xid);
            Assert.fail();
        } catch (TransactionAbortedException e) {

        }

        Field field = tm.getClass().getDeclaredField("hold_list");// 获取PrivateClass所有属性
        field.setAccessible(true);

        Hashtable<Integer, List<ResourceManager>> hold_list = (Hashtable<Integer, List<ResourceManager>>) field.get(tm);

        Assert.assertFalse(hold_list.containsKey(xid));

    }

    @Test
    public void testAbort() throws RemoteException, InvalidTransactionException, NoSuchFieldException, IllegalAccessException {
        int xid = tm.start();
        System.out.println(xid);
        context.checking(new Expectations() {
            {
                exactly(1).of(rmflights).prepare(xid);
                will(returnValue(true));

                exactly(1).of(rmcars).prepare(xid);
                will(returnValue(false));

                exactly(1).of(rmrooms).prepare(xid);
                will(returnValue(true));

                exactly(1).of(rmcustomers).prepare(xid);
                will(returnValue(true));

                exactly(1).of(rmflights).abort(xid);

                exactly(1).of(rmflights).getID();
                will(returnValue(ResourceManager.RMINameFlights));

                exactly(1).of(rmcars).abort(xid);

                exactly(1).of(rmcars).getID();
                will(returnValue(ResourceManager.RMINameCars));

                exactly(1).of(rmrooms).abort(xid);

                exactly(1).of(rmrooms).getID();
                will(returnValue(ResourceManager.RMINameRooms));

                exactly(1).of(rmcustomers).abort(xid);

                exactly(1).of(rmcustomers).getID();
                will(returnValue(ResourceManager.RMINameCustomers));
            }
        });


        tm.enlist(xid, rmflights);
        tm.enlist(xid, rmcars);
        tm.enlist(xid, rmrooms);
        tm.enlist(xid, rmcustomers);

        try {
            tm.commit(xid);
            Assert.fail();
        } catch (TransactionAbortedException e) {

        }

        Field field = tm.getClass().getDeclaredField("hold_list");// 获取PrivateClass所有属性
        field.setAccessible(true);

        Hashtable<Integer, List<ResourceManager>> hold_list = (Hashtable<Integer, List<ResourceManager>>) field.get(tm);

        Assert.assertFalse(hold_list.containsKey(xid));
    }


}
