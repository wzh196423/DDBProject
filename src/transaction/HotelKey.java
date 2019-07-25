package transaction;

import java.io.Serializable;

/**
 * Created by wangziheng on 2019/7/25.
 */
public class HotelKey implements Serializable {

    String location;
    double price;
    int numRooms;
    int numAvail;
    boolean isdeleted = false;

    public HotelKey(){
        this.location = "";
        this.price = 0.0;
        this.numRooms = 0;
        this.numAvail = 0;
    }
    public HotelKey(String location, double price, int numRooms, int numAvail){
        this.location = location;
        this.price = price;
        this.numRooms = numRooms;
        this.numAvail = numAvail;
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof CarKey))
            return false;
        if (this == o)
            return true;
        HotelKey k = (HotelKey) o;
        if (k.location.equals(this.location) && k.price == this.price && k.numRooms == this.numRooms && k.numAvail == this.numAvail)
            return true;
        return false;
    }

    public int hashCode() {
        return location.hashCode() + (int)price + numRooms + numAvail;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("[");
        buf.append("location=");
        buf.append(location);
        buf.append(";");
        buf.append("price=");
        buf.append(price);
        buf.append(";");
        buf.append("numRooms=");
        buf.append(numRooms);
        buf.append(";");
        buf.append("numAvail=");
        buf.append(numAvail);
        buf.append("]");

        return buf.toString();
    }
}
