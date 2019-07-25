package transaction;

import java.io.Serializable;

/**
 * Created by wangziheng on 2019/7/25.
 */
public class Customer implements Serializable, ResourceItem {
    public static final String INDEX_CUSTNAME = "custName";
    String custName;
    boolean isdeleted = false;

    public Customer() {
        this.custName="";
    }
    public Customer(String custName) {
        this.custName= custName;
    }

    @Override
    public String[] getColumnNames() {
        return new String[]{"custName"};
    }

    @Override
    public String[] getColumnValues() {
        return new String[]{custName};
    }

    @Override
    public Object getIndex(String indexName) throws InvalidIndexException {
        if (indexName.equals(INDEX_CUSTNAME)){
            return custName;
        }else {
            throw new InvalidIndexException(indexName);
        }
    }

    public String getCustName() {
        return custName;
    }

    @Override
    public Object getKey() {
        return new CustomerKey(custName);
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
        Customer customer = new Customer(getCustName());
        customer.isdeleted = isdeleted;
        return customer;
    }
}
