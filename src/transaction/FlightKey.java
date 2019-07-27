package transaction;

import java.io.Serializable;

/**
 * Created by wangziheng on 2019/7/25.
 */
public class FlightKey implements Serializable {
    String flightNum;
    int price;
    int numSeats;
    int numAvail;

    public FlightKey(){
        this.flightNum = "";
        this.price = 0;
        this.numSeats = 0;
        this.numAvail = 0;
    }

    public FlightKey(String flightNum, int price, int numSeats, int numAvail) {
        this.flightNum = flightNum;
        this.price = price;
        this.numSeats = numSeats;
        this.numAvail = numAvail;
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof CarKey))
            return false;
        if (this == o)
            return true;
        FlightKey k = (FlightKey) o;
        if (k.flightNum.equals(this.flightNum) && k.price == this.price && k.numSeats == this.numSeats && k.numAvail == this.numAvail)
            return true;
        return false;
    }

    public int hashCode() {
        return flightNum.hashCode() + price + numSeats + numAvail;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("[");
        buf.append("flightNum=");
        buf.append(flightNum);
        buf.append(";");
        buf.append("price=");
        buf.append(price);
        buf.append(";");
        buf.append("numSeats=");
        buf.append(numSeats);
        buf.append(";");
        buf.append("numAvail=");
        buf.append(numAvail);
        buf.append("]");

        return buf.toString();
    }
}
