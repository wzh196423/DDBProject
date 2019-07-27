package transaction;

import java.io.Serializable;

/**
 * Created by wangziheng on 2019/7/25.
 */
public class Flight implements Serializable, ResourceItem {
    public static final String INDEX_FLIGHTNUM = "flightNum";
    String flightNum;
    int price;
    int numSeats;
    int numAvail;
    boolean isdeleted = false;

    public Flight(){
        this.flightNum = "";
        this.price = 0;
        this.numSeats = 0;
        this.numAvail = 0;
    }
    public Flight(String flightNum, int price, int numSeats, int numAvail){
        this.flightNum = flightNum;
        this.price = price;
        this.numSeats = numSeats;
        this.numAvail = numAvail;
    }

    @Override
    public String[] getColumnNames() {
        return new String[]{ "flightNum", "price", "numSeats", "numAvail"};
    }

    @Override
    public String[] getColumnValues() {
        return new String[]{ flightNum, ""+price, ""+numSeats, ""+numAvail};
    }

    @Override
    public Object getIndex(String indexName) throws InvalidIndexException {
        if (indexName.equals(INDEX_FLIGHTNUM)){
            return flightNum;
        }else {
            throw new InvalidIndexException(indexName);
        }
    }

    public String getFlightNum() {
        return flightNum;
    }

    public void setFlightNum(String flightNum) {
        this.flightNum = flightNum;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getNumSeats() {
        return numSeats;
    }

    public void setNumSeats(int numSeats) {
        this.numSeats = numSeats;
    }

    public int getNumAvail() {
        return numAvail;
    }

    public void setNumAvail(int numAvail) {
        this.numAvail = numAvail;
    }

    @Override
    public Object getKey() {
        return flightNum;
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
        Flight flight = new Flight(getFlightNum(), getPrice(), getNumSeats(), getNumAvail());
        flight.isdeleted = isdeleted;
        return flight;
    }
}
