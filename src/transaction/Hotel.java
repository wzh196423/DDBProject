package transaction;

import java.io.Serializable;

/**
 * Created by wangziheng on 2019/7/25.
 */
public class Hotel implements Serializable, ResourceItem {
    public static final String INDEX_LOCATION = "location";
    String location;
    int price;
    int numRooms;
    int numAvail;
    boolean isdeleted = false;

    public Hotel(){
        this.location = "";
        this.price = 0;
        this.numRooms = 0;
        this.numAvail = 0;
    }
    public Hotel(String location, int price, int numRooms, int numAvail){
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

    public void setLocation(String location) {
        this.location = location;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getNumRooms() {
        return numRooms;
    }

    public void setNumRooms(int numRooms) {
        this.numRooms = numRooms;
    }

    public int getNumAvail() {
        return numAvail;
    }

    public void setNumAvail(int numAvail) {
        this.numAvail = numAvail;
    }

    @Override
    public Object getKey() {
        return location;    }

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
