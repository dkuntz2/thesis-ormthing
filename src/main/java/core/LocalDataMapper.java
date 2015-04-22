package co.kuntz.sqliteEngine.core;

import java.sql.*;

public class LocalDataMapper extends DataMapper {
    private LocalDataMapper() {
        System.out.println("Created LocalDataMapper");
    }

    public static void setDataMapperFactory() {
        DataMapper.factory = new DataMapper.DataMapperFactory() {
            @Override public DataMapper createMapper() {
                return new LocalDataMapper();
            }
        };
    }

    public void insert(String name, String object) {
        System.out.println("INSERT");
    }

    public Object retrieve(String name, Class<?> klass) {
        return "RETRIEVE";
    }
}
