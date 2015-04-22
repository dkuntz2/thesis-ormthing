package co.kuntz.sqliteEngine;

import co.kuntz.sqliteEngine.core.*;

public class Driver {
    public static void main(String[] args) {
        LocalDataMapper.setDataMapperFactory();

        DataMapper instance = DataMapper.getInstance();
        instance.insert("hi", "there");
    }
}
