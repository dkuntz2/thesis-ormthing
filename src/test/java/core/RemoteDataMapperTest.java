package co.kuntz.sqliteEngine.tests.core;

import co.kuntz.sqliteEngine.core.DataMapper;
import co.kuntz.sqliteEngine.core.RemoteDataMapper;
import co.kuntz.sqliteEngine.core.LocalDataMapper;
import co.kuntz.sqliteEngine.core.RemoteDataMapperServer;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class RemoteDataMapperTest {
    private static RemoteDataMapperServer server;
    private static Thread serverThread;

    @BeforeClass
    public static void beforeClass() {
        server = new RemoteDataMapperServer(":memory:");
        serverThread = new Thread(server);
        serverThread.start();

        try {
            Thread.sleep(400);
        } catch (Throwable t) {
            throw new RuntimeException("Couldn't sleep.");
        }
    }

    @AfterClass
    public static void afterClass() {
        server.stop();
    }

    @Before
    public void setup() {
        DataMapper.dbName = ":memory:";
        DataMapper.factory = RemoteDataMapper.getDataMapperFactory();
    }

    @After
    public void teardown() {
        //server.purgeData();
    }

    @Test
    public void testPrimitives() {
        DataMapper instance = DataMapper.getInstance();

        // TODO: don't hard code these. Turn them into variables.
        instance.put("one", 1);
        instance.put("twotwo", 2.2);
        instance.put("true", true);
        instance.put("string", "Hi There!");
        instance.put("char", 'c');

        //Map<String,String> all = instance.getAll();
        //for (Map.Entry<String,String> item: all.entrySet()) {
            //System.out.println(item.getKey() + " : " + item.getValue());
        //}

        int one = (int) instance.get("one", int.class);
        double twotwo = (double) instance.get("twotwo", double.class);
        boolean tru = (boolean) instance.get("true", boolean.class);
        String hiThere = (String) instance.get("string", String.class);
        char care = (char) instance.get("char", char.class);

        assertEquals(1, one);
        assertEquals(2.2, twotwo, 0);
        assertEquals(true, tru);
        assertEquals("Hi There!", hiThere);
        assertEquals('c', care);
    }
}
