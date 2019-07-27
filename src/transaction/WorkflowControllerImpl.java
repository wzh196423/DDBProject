package transaction;

import lockmgr.DeadlockException;

import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.*;

/**
 * Workflow Controller for the Distributed Travel Reservation System.
 * <p>
 * Description: toy implementation of the WC.  In the real
 * implementation, the WC should forward calls to either RM or TM,
 * instead of doing the things itself.
 */

public class WorkflowControllerImpl
        extends java.rmi.server.UnicastRemoteObject
        implements WorkflowController {

    protected int flightcounter, flightprice, carscounter, carsprice, roomscounter, roomsprice;
    protected int xidCounter;

    HashSet<Integer> transaction_list;

    protected ResourceManager rmFlights = null;
    protected ResourceManager rmRooms = null;
    protected ResourceManager rmCars = null;
    protected ResourceManager rmCustomers = null;
    protected TransactionManager tm = null;

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
        String rmiPort = prop.getProperty("wc.port");
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
            WorkflowControllerImpl obj = new WorkflowControllerImpl();
            Naming.rebind(rmiPort + WorkflowController.RMIName, obj);
            System.out.println("WC bound");
        } catch (Exception e) {
            System.err.println("WC not bound:" + e);
            System.exit(1);
        }
    }

    public static WorkflowController init() throws RemoteException {
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
        String rmiPort = prop.getProperty("wc.port");
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

        WorkflowControllerImpl obj = null;
        try {
            obj = new WorkflowControllerImpl();
            Naming.rebind(rmiPort + WorkflowController.RMIName, obj);
            System.out.println("WC bound");
        } catch (Exception e) {
            System.err.println("WC not bound:" + e);
            System.exit(1);
        }

        return obj;
    }


    public WorkflowControllerImpl() throws RemoteException {
        flightcounter = 0;
        flightprice = 0;
        carscounter = 0;
        carsprice = 0;
        roomscounter = 0;
        roomsprice = 0;
        flightprice = 0;

        xidCounter = 1;
        transaction_list = new HashSet<>();

        while (!reconnect()) {
            // would be better to sleep a while
        }
    }


    // TRANSACTION INTERFACE
    public int start()
            throws RemoteException {
        int xid = tm.start();
        transaction_list.add(xid);
        return xid;
    }

    public boolean commit(int xid)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to commit");
        }
        if (tm.commit(xid)){
            System.out.println("xid = " + xid + " commit successfully!");
            transaction_list.remove(xid);
            return true;
        }
        return false;
    }

    public void abort(int xid)
            throws RemoteException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to abort");
        }
        tm.abort(xid);
        transaction_list.remove(xid);
        return;
    }


    // ADMINISTRATIVE INTERFACE
    public boolean addFlight(int xid, String flightNum, int numSeats, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to addFlight");
        }
        if (flightNum == null || numSeats < 0){
            return false;
        }
        try {
            Flight flight = (Flight) rmFlights.query(xid, rmFlights.getID(), flightNum);
            if (flight != null){
                flight.numSeats += numSeats;
                if(price > 0){
                    flight.setPrice(price);
                }
                return rmFlights.update(xid, rmFlights.getID(), flightNum, flight);
            }else{
                flight = new Flight(flightNum, price > 0 ? price : 0, numSeats, numSeats);
                return rmFlights.insert(xid, rmFlights.getID(), flight);
            }

        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public boolean deleteFlight(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to deleteFlight");
        }
        try {
            Flight flight = (Flight) rmFlights.query(xid, rmFlights.getID(), flightNum);
            if (flight == null){
                return false;
            } else{
                // will fail if the flight has reservations
                Collection<Reservation> revs = rmCustomers.query(xid, ResourceManager.RESERVATION_TABLENAME, Reservation.INDEX_RESVKEY, flightNum);
                if (!revs.isEmpty())
                    return false;
                return rmFlights.delete(xid, rmFlights.getID(), Flight.INDEX_FLIGHTNUM, flightNum) != 0;
            }
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        } catch (InvalidIndexException e) {
            System.err.println("Error! Cannot find reservations associated with the flight which num = " + flightNum);
            return false;
        }
    }

    public boolean addRooms(int xid, String location, int numRooms, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to addRooms");
        }
        if (location == null || numRooms < 0){
            return false;
        }
        try {
            Hotel hotel = (Hotel) rmRooms.query(xid, rmRooms.getID(), location);
            if (hotel != null){
                hotel.numRooms += numRooms;
                if(price > 0){
                    hotel.setPrice(price);
                }
                return rmRooms.update(xid, rmRooms.getID(), location, hotel);
            }else{
                hotel = new Hotel(location, price > 0 ? price : 0, numRooms, numRooms);
                return rmRooms.insert(xid, rmRooms.getID(), hotel);
            }

        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public boolean deleteRooms(int xid, String location, int numRooms)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to deleteRooms");
        }
        if (location == null || numRooms < 0){
            return false;
        }
        try{
            Hotel hotel = (Hotel) rmRooms.query(xid, rmRooms.getID(), location);
            if (hotel == null)
                return false;
            if (hotel.getNumAvail() < numRooms)
                return false;
            // delete both total and available
            hotel.setNumRooms(hotel.getNumRooms() - numRooms);
            hotel.setNumAvail(hotel.getNumAvail() - numRooms);
            return rmRooms.update(xid, rmRooms.getID(), location, hotel);
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public boolean addCars(int xid, String location, int numCars, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to addCars");
        }
        if (location == null || numCars < 0){
            return false;
        }
        try {
            Car car = (Car) rmCars.query(xid, rmCars.getID(), location);
            if (car != null){
                car.numCars += numCars;
                if(price > 0){
                    car.setPrice(price);
                }
                return rmCars.update(xid, rmCars.getID(), location, car);
            }else{
                car = new Car(location, price > 0 ? price : 0, numCars, numCars);
                return rmCars.insert(xid, rmCars.getID(), car);
            }
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public boolean deleteCars(int xid, String location, int numCars)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to deleteCars");
        }
        if (location == null || numCars < 0){
            return false;
        }
        try{
            Car car = (Car) rmCars.query(xid, rmCars.getID(), location);
            if (car == null)
                return false;
            if (car.getNumAvail() < numCars)
                return false;
            // delete both total and available
            car.setNumCars(car.getNumCars() - numCars);
            car.setNumAvail(car.getNumAvail() - numCars);
            return rmCars.update(xid, rmCars.getID(), location, car);
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public boolean newCustomer(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to newCustomer");
        }
        if (custName == null)
            return false;
        try {
            Customer customer = (Customer) rmCustomers.query(xid, rmCustomers.getID(), custName);
            if (customer != null){
                // means already exists
                return true;
            } else {
                // means not exist
                customer = new Customer(custName);
                return rmCustomers.insert(xid, rmCustomers.getID(), customer);
            }
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());        }
    }

    public boolean deleteCustomer(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to deleteCustomer");
        }
        if (custName == null)
            return false;
        try {
            Customer customer = (Customer) rmCustomers.query(xid, rmCustomers.getID(), custName);
            if (customer == null)
                return false;
            // un-book all the reservations first
            Collection<Reservation> resvs = rmCustomers.query(xid, ResourceManager.RESERVATION_TABLENAME, Reservation.INDEX_CUSTNAME, custName);
            for (Reservation reservation : resvs){
                switch (reservation.getResvType()){
                    case Reservation.RESERVATION_TYPE_FLIGHT: {
                        Flight flight = (Flight) rmFlights.query(xid, rmFlights.getID(), reservation.getKey());
                        if (flight != null) {
                            flight.setNumAvail(flight.getNumAvail() + 1);
                            rmFlights.update(xid, rmFlights.getID(), flight.getFlightNum(), flight);
                            break;
                        }
                    }
                    case Reservation.RESERVATION_TYPE_HOTEL:{
                        Hotel hotel = (Hotel) rmRooms.query(xid, rmRooms.getID(), reservation.getKey());
                        if (hotel != null) {
                            hotel.setNumAvail(hotel.getNumAvail() + 1);
                            rmRooms.update(xid, rmRooms.getID(), hotel.getLocation(), hotel);
                            break;
                        }
                    }
                    case Reservation.RESERVATION_TYPE_CAR:{
                        Car car = (Car) rmCars.query(xid, rmCars.getID(), reservation.getKey());
                        if (car != null) {
                            car.setNumAvail(car.getNumAvail() + 1);
                            rmCars.update(xid, rmCars.getID(), car.getLocation(), car);
                            break;
                        }
                    }
                }
            }

            // after un-book successfully, delete all reservations associated with the customer
            rmCustomers.delete(xid, ResourceManager.RESERVATION_TABLENAME, Reservation.INDEX_CUSTNAME, custName);

            // finally, delete the customer
            return rmCustomers.delete(xid, rmCustomers.getID(), custName);

        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        } catch (InvalidIndexException e) {
            System.err.println("Error! Cannot find reservations associated with the customer which name = " + custName);
            return false;
        }
    }


    // QUERY INTERFACE
    public int queryFlight(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to queryFlight");
        }
        if (flightNum == null)
            return -1;
        try{
            Flight flight = (Flight) rmFlights.query(xid, rmFlights.getID(), flightNum);
            if (flight == null)
                return -1;
            return flight.getNumAvail();
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public int queryFlightPrice(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)) {
            throw new InvalidTransactionException(xid, "Have no xid here to queryFlightPrice");
        }
        if (flightNum == null)
            return -1;
        try {
            Flight flight = (Flight) rmFlights.query(xid, rmFlights.getID(), flightNum);
            if (flight == null)
                return -1;
            return flight.getPrice();
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = " + xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public int queryRooms(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to queryRooms");
        }
        if (location == null)
            return -1;
        try{
            Hotel hotel = (Hotel) rmRooms.query(xid, rmRooms.getID(), location);
            if (hotel == null)
                return -1;
            return hotel.getNumAvail();
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public int queryRoomsPrice(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to queryRoomsPrice");
        }
        if (location == null)
            return -1;
        try{
            Hotel hotel = (Hotel) rmRooms.query(xid, rmRooms.getID(), location);
            if (hotel == null)
                return -1;
            return hotel.getPrice();
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public int queryCars(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to queryCars");
        }
        if (location == null)
            return -1;
        try{
            Car car = (Car) rmCars.query(xid, rmCars.getID(), location);
            if (car == null)
                return -1;
            return car.getNumAvail();
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public int queryCarsPrice(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to queryCarsPrice");
        }
        if (location == null)
            return -1;
        try{
            Car car = (Car) rmCars.query(xid, rmCars.getID(), location);
            if (car == null)
                return -1;
            return car.getPrice();
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public int queryCustomerBill(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to queryCustomerBill");
        }
        if (custName == null)
            return -1;

        try{
            Customer customer = (Customer) rmCustomers.query(xid, rmCustomers.getID(), custName);
            if (customer == null)
                return -1;

            // query all the bills
            int totolBill = 0;
            Collection<Reservation> reservations = rmCustomers.query(xid, ResourceManager.RESERVATION_TABLENAME, Reservation.INDEX_CUSTNAME, custName);
            for (Reservation reservation : reservations){
                switch (reservation.getResvType()){
                    case Reservation.RESERVATION_TYPE_FLIGHT:{
                        Flight flight = (Flight) rmFlights.query(xid, rmFlights.getID(), reservation.getKey());
                        if (flight != null)
                            totolBill += flight.getPrice();
                        break;
                    }
                    case Reservation.RESERVATION_TYPE_HOTEL:{
                        Hotel hotel = (Hotel) rmRooms.query(xid, rmRooms.getID(), reservation.getKey());
                        if (hotel != null)
                            totolBill += hotel.getPrice();
                        break;
                    }
                    case Reservation.RESERVATION_TYPE_CAR:{
                        Car car = (Car) rmCars.query(xid, rmCars.getID(), reservation.getKey());
                        if (car != null)
                            totolBill += car.getPrice();
                        break;
                    }
                }
            }
            return totolBill;
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        } catch (InvalidIndexException e) {
            System.err.println("Error! Cannot find reservations associated with the customer which name = " + custName);
            return -1;
        }
    }


    // RESERVATION INTERFACE
    public boolean reserveFlight(int xid, String custName, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            System.err.println("nonono");
            throw new InvalidTransactionException(xid, "Have no xid here to queryCustomerBill");
        }
        if (custName == null || flightNum == null)
            return false;
        try{
            Flight flight = (Flight) rmFlights.query(xid, rmFlights.getID(), flightNum);
            if (flight == null || flight.getNumAvail() <= 0) {
                System.err.println("Have no flight or have no sears in this flight (num = " + flightNum + ")");
                return false;
            }
            Customer customer = (Customer) rmCustomers.query(xid, rmCustomers.getID(), custName);
            if (customer == null) {
                System.err.println("Have no customer whose name is " + custName);
                return false;
            }
            // nothing wrong
            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_FLIGHT, flightNum);

            // we're supposed to update the flight only after the insert succeeded
            if (rmCustomers.insert(xid, ResourceManager.RESERVATION_TABLENAME, reservation)){
                flight.setNumAvail(flight.getNumAvail() - 1);
                if (rmFlights.update(xid, rmFlights.getID(), flightNum, flight))
                    return true;
            }
            System.err.println("Other bugs");
            return false;
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public boolean reserveCar(int xid, String custName, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to queryCustomerBill");
        }
        if (custName == null || location == null)
            return false;
        try{
            Car car = (Car) rmCars.query(xid, rmCars.getID(), location);
            if (car == null || car.getNumAvail() <= 0)
                return false;
            Customer customer = (Customer) rmCustomers.query(xid, rmCustomers.getID(), custName);
            if (customer == null)
                return false;
            // nothing wrong
            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_CAR, location);

            // we're supposed to update the flight only after the insert succeeded
            if (rmCustomers.insert(xid, ResourceManager.RESERVATION_TABLENAME, reservation)){
                car.setNumAvail(car.getNumAvail() - 1);
                if (rmCars.update(xid, rmCars.getID(), location, car))
                    return true;
            }
            return false;
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    public boolean reserveRoom(int xid, String custName, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (!transaction_list.contains(xid)){
            throw new InvalidTransactionException(xid, "Have no xid here to queryCustomerBill");
        }
        if (custName == null || location == null)
            return false;
        try{
            Hotel hotel = (Hotel) rmRooms.query(xid, rmRooms.getID(), location);
            if (hotel == null || hotel.getNumAvail() <= 0)
                return false;
            Customer customer = (Customer) rmCustomers.query(xid, rmCustomers.getID(), custName);
            if (customer == null)
                return false;
            // nothing wrong
            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_HOTEL, location);

            // we're supposed to update the flight only after the insert succeeded
            if (rmCustomers.insert(xid, ResourceManager.RESERVATION_TABLENAME, reservation)){
                hotel.setNumAvail(hotel.getNumAvail() - 1);
                if (rmRooms.update(xid, rmRooms.getID(), location, hotel))
                    return true;
            }
            return false;
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction (xid = "+ xid + ") cause dead lock: " + e.getMessage());
        }
    }

    // TECHNICAL/TESTING INTERFACE
    public boolean reconnect()
            throws RemoteException {
        Properties prop = new Properties();
        try
        {
            prop.load(new FileInputStream("conf/ddb.conf"));
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
            return false;
        }
        String rmiPort = prop.getProperty("tm.port");
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        try {
            rmFlights =
                    (ResourceManager) Naming.lookup(rmiPort +
                            ResourceManager.RMINameFlights);
            System.out.println("WC bound to RMFlights");
            rmRooms =
                    (ResourceManager) Naming.lookup(rmiPort +
                            ResourceManager.RMINameRooms);
            System.out.println("WC bound to RMRooms");
            rmCars =
                    (ResourceManager) Naming.lookup(rmiPort +
                            ResourceManager.RMINameCars);
            System.out.println("WC bound to RMCars");
            rmCustomers =
                    (ResourceManager) Naming.lookup(rmiPort +
                            ResourceManager.RMINameCustomers);
            System.out.println("WC bound to RMCustomers");
            tm =
                    (TransactionManager) Naming.lookup(rmiPort +
                            TransactionManager.RMIName);
            System.out.println("WC bound to TM");
        } catch (Exception e) {
            System.err.println("WC cannot bind to some component:" + e);
            return false;
        }

        try {
            if (rmFlights.reconnect() && rmRooms.reconnect() &&
                    rmCars.reconnect() && rmCustomers.reconnect()) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("Some RM cannot reconnect:" + e);
            return false;
        }

        return false;
    }

    public boolean dieNow(String who)
            throws RemoteException {
        if (who.equals(TransactionManager.RMIName) ||
                who.equals("ALL")) {
            try {
                tm.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameFlights) ||
                who.equals("ALL")) {
            try {
                rmFlights.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameRooms) ||
                who.equals("ALL")) {
            try {
                rmRooms.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameCars) ||
                who.equals("ALL")) {
            try {
                rmCars.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameCustomers) ||
                who.equals("ALL")) {
            try {
                rmCustomers.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(WorkflowController.RMIName) ||
                who.equals("ALL")) {
            System.exit(1);
        }
        return true;
    }

    private boolean setDieTime(String who, String time) throws RemoteException{

        switch (who){
            case ResourceManager.RMINameCars:
                rmCars.setDieTime(time);
                break;
            case ResourceManager.RMINameCustomers:
                rmCustomers.setDieTime(time);
                break;
            case ResourceManager.RMINameFlights:
                rmFlights.setDieTime(time);
                break;
            case ResourceManager.RMINameRooms:
                rmRooms.setDieTime(time);
                break;
            default:
                System.out.println("Wrong RM name = " + who);
                return false;
        }
        return true;
    }

    public boolean dieRMAfterEnlist(String who)
            throws RemoteException {
        return setDieTime(who, "AfterEnlist");
    }

    public boolean dieRMBeforePrepare(String who)
            throws RemoteException {
        return setDieTime(who, "BeforePrepare");
    }

    public boolean dieRMAfterPrepare(String who)
            throws RemoteException {
        return setDieTime(who, "AfterPrepare");
    }

    public boolean dieTMBeforeCommit()
            throws RemoteException {
        tm.setDieTime("BeforeCommit");
        return true;
    }

    public boolean dieTMAfterCommit()
            throws RemoteException {
        tm.setDieTime("AfterCommit");
        return true;
    }

    public boolean dieRMBeforeCommit(String who)
            throws RemoteException {
        return setDieTime(who, "BeforeCommit");
    }

    public boolean dieRMBeforeAbort(String who)
            throws RemoteException {
        return setDieTime(who, "BeforeAbort");
    }
}
