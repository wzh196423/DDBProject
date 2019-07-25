package transaction;

import java.io.Serializable;

/**
 * Created by wangziheng on 2019/7/25.
 */
public class CustomerKey implements Serializable{

    String custName;

    public CustomerKey() {
        this.custName="";
    }
    public CustomerKey(String custName) {
        this.custName= custName;
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof CarKey))
            return false;
        if (this == o)
            return true;
        CustomerKey k = (CustomerKey) o;
        if (k.custName.equals(this.custName))
            return true;
        return false;
    }

    public int hashCode() {
        return custName.hashCode();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("[");
        buf.append("custName=");
        buf.append(custName);
        buf.append("]");

        return buf.toString();
    }
}
