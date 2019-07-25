package transaction;

import java.io.Serializable;

/**
 * Created by wangziheng on 2019/7/25.
 */
public class Car implements Serializable, ResourceItem {
    public static final String INDEX_LOCATION = "location";
    String location;
    double price;
    int numCars;
    int numAvail;
    boolean isdeleted = false;
    public Car(){
        this.location = "";
        this.price = 0.0;
        this.numAvail = 0;
        this.numCars = 0;
    }
    public Car(String location, double price, int numCars, int numAvail){
        this.location = location;
        this.price = price;
        this.numCars = numCars;
        this.numAvail = numAvail;
    }

    @Override
    public String[] getColumnNames() {
        return new String[]{"location", "price", "numCars", "numAvail"};
    }

    @Override
    public String[] getColumnValues() {
        return new String[]{location, ""+price, ""+numCars, ""+numAvail};
    }

    @Override
    public Object getIndex(String indexName) throws InvalidIndexException {
        if (indexName.equals(INDEX_LOCATION)){
            return location;
        }else {
            throw new InvalidIndexException(indexName);
        }
    }

    @Override
    public Object getKey() {
        return new CarKey(location, price, numCars, numAvail);
    }

    public String getLocation() {
        return location;
    }

    public double getPrice() {
        return price;
    }

    public int getNumCars() {
        return numCars;
    }

    public int getNumAvail() {
        return numAvail;
    }

    @Override
    public boolean isDeleted() {
        return isdeleted;
    }

    @Override
    public void delete() {
        isdeleted = true;
    }

    @Override
    public Object clone() {
        Car car = new Car(getLocation(), getPrice(), getNumCars(), getNumAvail());
        car.isdeleted = isdeleted;
        return car;
    }
}
