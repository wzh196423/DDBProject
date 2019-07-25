package transaction;

import java.io.Serializable;

/**
 * Created by wangziheng on 2019/7/25.
 */
public class CarKey implements Serializable {
    String location;
    double price;
    int numCars;
    int numAvail;

    public CarKey(){
        this.location = "";
        this.price = 0.0;
        this.numAvail = 0;
        this.numCars = 0;
    }

    public CarKey(String location, double price, int numCars, int numAvail){
        this.location = location;
        this.price = price;
        this.numCars = numCars;
        this.numAvail = numAvail;
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof CarKey))
            return false;
        if (this == o)
            return true;
        CarKey k = (CarKey) o;
        if (k.location.equals(this.location) && k.price == this.price && k.numCars == this.numCars && k.numAvail == this.numAvail)
            return true;
        return false;
    }

    public int hashCode() {
        return location.hashCode() + (int)price + numCars + numAvail;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("[");
        buf.append("location=");
        buf.append(location);
        buf.append(";");
        buf.append("price=");
        buf.append(price);
        buf.append(";");
        buf.append("numCars=");
        buf.append(numCars);
        buf.append(";");
        buf.append("numAvail=");
        buf.append(numAvail);
        buf.append("]");

        return buf.toString();
    }
}
