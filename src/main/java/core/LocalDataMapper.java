package co.kuntz.sqliteEngine.core;

import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

public class LocalDataMapper extends DataMapper {
    private Gson gson = new Gson();
    private Connection connection;

    private PreparedStatement insertStatement;
    private PreparedStatement retrieveStatement;
    private PreparedStatement deleteStatement;

    private LocalDataMapper() {
        this(DataMapper.dbName);
    }

    private LocalDataMapper(String dbName) {
        try {
            Class.forName("org.sqlite.JDBC");

            String revisedDbName = dbName;
            if (!revisedDbName.equals(":memory:") && revisedDbName.endsWith(".db")) {
                revisedDbName = revisedDbName + ".db";
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("create table if not exists items(name text primary key, object text)");

            insertStatement = connection.prepareStatement("insert into items values(?, ?)");
            retrieveStatement = connection.prepareStatement("select name, object from items where name = ?");
            deleteStatement = connection.prepareStatement("delete from items where name = ?");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static DataMapper.DataMapperFactory getDataMapperFactory(final String name) {
        return new DataMapper.DataMapperFactory() {
            @Override public DataMapper createMapper() {
                return new LocalDataMapper(name);
            }
        };
    }

    public static DataMapper.DataMapperFactory getDataMapperFactory() {
        return getDataMapperFactory(DataMapper.dbName);
    }

    public void put(String name, Object obj) {
        try {
            insertStatement.setString(1, name);
            insertStatement.setString(2, gson.toJson(obj));
            insertStatement.executeUpdate();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public void putRaw(String name, String obj) {
        try {
            insertStatement.setString(1, name);
            insertStatement.setString(2, obj);
            insertStatement.executeUpdate();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public Object get(String name, Class<?> klass) {
        try {
            retrieveStatement.setString(1, name);
            ResultSet result = retrieveStatement.executeQuery();
            if (!result.next()) {
                return null;
            }

            return gson.fromJson(result.getString("object"), klass);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public String getString(String name) {
        try {
            retrieveStatement.setString(1, name);
            ResultSet result = retrieveStatement.executeQuery();
            if (!result.next()) {
                return null;
            }

            return result.getString("object");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public void delete(String itemName) {
        try {
            deleteStatement.setString(1, itemName);
            deleteStatement.executeUpdate();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public Map<String, String> getAll() {
        try {
            Statement statement = connection.createStatement();
            ResultSet query = statement.executeQuery("select * from items");

            Map<String, String> results = new HashMap<>();
            while (query.next()) {
                results.put(query.getString("name"), query.getString("object"));
            }

            return results;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public void purge() {
        try {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("drop table items");
            stmt.executeUpdate("create table if not exists items(name text primary key, object text)");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
