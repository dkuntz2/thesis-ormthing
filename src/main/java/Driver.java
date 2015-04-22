package co.kuntz.sqliteEngine;

import co.kuntz.sqliteEngine.core.*;
import java.util.Arrays;

public class Driver {
    public static void main(String[] args) {
        LocalDataMapper.setDataMapperFactory();

        DataMapper instance = DataMapper.getInstance();
        //instance.insert("hi", "there");

        //String hiThere = (String) instance.retrieve("hi", String.class);

        //instance.insert("one", "Some fucking string thing");
        //instance.insert("two", new String[] {"Hello", "World", "Array!"});
        //instance.insert("three", "This should be a map...");

        //((LocalDataMapper)instance).showall();

        String hiThere = (String) instance.get("hi", String.class);
        System.out.println(hiThere);

        String[] two = (String[]) instance.get("two", String[].class);
        System.out.println(Arrays.toString(two));
    }
}
