package transaction;

import java.io.Serializable;

/**
 * Created by wangziheng on 2019/7/25.
 */
public class Hotel implements Serializable, ResourceItem {
    public static final String INDEX_LOCATION = "location";
    String location;
    double price;
    int numRooms;
    int numAvail;
    boolean isdeleted = false;

    public Hotel(){
        this.location = "";
        this.price = 0.0;
        this.numRooms = 0;
        this.numAvail = 0;
    }
    public Hotel(String location, double price, int numRooms, int numAvail){
        this.location = location;
        this.price = price;
        this.numRooms = numRooms;
        this.numAvail = numAvail;
    }

    @Override
    public String[] getColumnNames() {
        return new String[]{"location", "price", "numRooms", "numAvail"};
    }

    @Override
    public String[] getColumnValues() {
        return new String[]{location, ""+price, ""+numRooms, ""+numAvail};
    }

    @Override
    public Object getIndex(String indexName) throws InvalidIndexException {
        if (indexName.equals(INDEX_LOCATION)){
            return location;
        }else {
            throw new InvalidIndexException(indexName);
        }
    }

    public String getLocation() {
        return location;
    }

    public double getPrice() {
        return price;
    }

    public int getNumRooms() {
        return numRooms;
    }

    public int getNumAvail() {
        return numAvail;
    }

    @Override
    public Object getKey() {
        return new HotelKey(location, price, numRooms, numAvail);    }

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
        Hotel hotel = new Hotel(getLocation(), getPrice(), getNumRooms(), getNumAvail());
        hotel.isdeleted = isdeleted;
        return hotel;
    }
}
