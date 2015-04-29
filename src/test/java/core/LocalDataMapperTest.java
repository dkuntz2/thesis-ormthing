package co.kuntz.sqliteEngine.tests.core;

import co.kuntz.sqliteEngine.core.DataMapper;
import co.kuntz.sqliteEngine.core.LocalDataMapper;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.junit.Before;

import java.util.*;

public class LocalDataMapperTest {
    private DataMapper instance;

    @Before
    public void setup() {
        instance = LocalDataMapper.getDataMapperFactory(":memory:").createMapper();
    }

    @Test
    public void testPrimitives() {
        // TODO: don't hard code these. Turn them into variables.
        instance.put("one", 1);
        instance.put("twotwo", 2.2);
        instance.put("true", true);
        instance.put("string", "Hi There!");
        instance.put("char", 'c');

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

    @Test
    public void testArrays() {
        instance.put("ints", new int[] {1, 2, 3});
        instance.put("doubles", new double[] {1.1, 2.2, 3.3});
        instance.put("booleans", new boolean[] {true, false, true});
        instance.put("strings", new String[] {"first", "second", "third"});
        instance.put("chars", new char[] {'a', 'b', 'c'});

        int[] ints = (int[]) instance.get("ints", int[].class);
        double[] doubles = (double[]) instance.get("doubles", double[].class);
        boolean[] bools = (boolean[]) instance.get("booleans", boolean[].class);
        String[] strings = (String[]) instance.get("strings", String[].class);
        char[] chars = (char[]) instance.get("chars", char[].class);

        assertArrayEquals(new int[] {1, 2, 3}, ints);
        assertTrue(Arrays.equals(new double[] {1.1, 2.2, 3.3}, doubles));
        assertArrayEquals(new boolean[] {true, false, true}, bools);
        assertArrayEquals(new String[] {"first", "second", "third"}, strings);
        assertArrayEquals(new char[] {'a', 'b', 'c'}, chars);
    }

    class BundleOfJunk {
        public int integer;
        public String string;
        public List<String> strings;
        public Map<Integer, String> map;

        @Override public boolean equals(Object o) {
            if (o == null || !(o instanceof BundleOfJunk)) {
                return false;
            }

            BundleOfJunk that = (BundleOfJunk) o;

            return that.integer == this.integer &&
                that.string.equals(this.string) &&
                that.strings.equals(this.strings) &&
                that.map.equals(this.map);
        }
    }

    @Test
    public void testArbirtraryClass() {
        BundleOfJunk a = new BundleOfJunk();
        a.integer = 4;
        a.string = "wtf?";
        a.strings = new ArrayList<String>();
        a.strings.add("a");
        a.strings.add("b");
        a.strings.add("c");
        a.map = new HashMap<Integer, String>();
        a.map.put(1, "Hello World");
        a.map.put(2, "Goodbye World");

        instance.put("junk", a);

        // FUCK YOU JUNIT!
        assertTrue(a.equals(instance.get("junk", BundleOfJunk.class)));
    }

    @Test
    public void testDeletion() {
        instance.put("deleteme", "whotfcares?");
        instance.delete("deleteme");

        assertNull(instance.get("deleteme", String.class));
        assertNull(instance.getString("deleteme"));
    }

    @Test
    public void testReturnsNullForNonExistantData() {
        assertNull(instance.getString("notathing"));
        assertNull(instance.get("notathing", Object.class));
    }
}
