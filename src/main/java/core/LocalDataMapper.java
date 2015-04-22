package co.kuntz.sqliteEngine.core;

import java.sql.*;
import com.google.gson.Gson;

public class LocalDataMapper extends DataMapper {
    private Gson gson = new Gson();
    private Connection connection;

    private PreparedStatement insertStatement;
    private PreparedStatement retrieveStatement;

    private LocalDataMapper() {
        try {
            Class.forName("org.sqlite.JDBC");

            connection = DriverManager.getConnection("jdbc:sqlite:test.db");
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("create table if not exists items(name text primary key, object text)");

            insertStatement = connection.prepareStatement("insert into items values(?, ?)");
            retrieveStatement = connection.prepareStatement("select name, object from items where name = ?");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void setDataMapperFactory() {
        DataMapper.factory = new DataMapper.DataMapperFactory() {
            @Override public DataMapper createMapper() {
                return new LocalDataMapper();
            }
        };
    }

    public void insert(String name, Object obj) {
        try {
            insertStatement.setString(1, name);
            insertStatement.setString(2, gson.toJson(obj));
            insertStatement.executeUpdate();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public Object retrieve(String name, Class<?> klass) {
        try {
            retrieveStatement.setString(1, name);
            ResultSet result = retrieveStatement.executeQuery();
            result.next();

            return gson.fromJson(result.getString("object"), klass);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // TESTING: TODO: delete this
    public void showall() {
        try {
            ResultSet results = connection.createStatement().executeQuery("select * from items");
            while (results.next()) {
                System.out.println(results.getString("name") + " -> " + results.getString("object"));
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
