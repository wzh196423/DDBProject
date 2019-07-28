package test;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.BeforeClass;
import org.junit.Test;
import transaction.ResourceManager;
import transaction.TransactionManager;

/**
 * Created by 14302 on 2019/7/28.
 */
public class WCTest {
    private static TransactionManager tm;
    private ResourceManager rmflights;
    private ResourceManager rmcars;
    private ResourceManager rmrooms;
    private ResourceManager rmcustomers;

    private Mockery context = new JUnit4Mockery();

    @BeforeClass
    public void init() {

    }
}
