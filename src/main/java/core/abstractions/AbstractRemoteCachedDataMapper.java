package co.kuntz.sqliteEngine.core.abstractions;

import co.kuntz.sqliteEngine.core.RemoteDataMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;

import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.Calendar;

import com.google.gson.Gson;

public abstract class AbstractRemoteCachedDataMapper extends RemoteDataMapper {
    public static final String DEFAULT_DB_NAME = "cached.db";
    public static final long OUTDATED_CACHE_DELTA = TimeUnit.MINUTES.toMillis(30);

    private Connection connection;
    private long outdated_delta = OUTDATED_CACHE_DELTA;
    private Gson gson = new Gson();

    private PreparedStatement insertStatement;
    private PreparedStatement retrieveStatement;
    private PreparedStatement deleteStatement;
    private PreparedStatement updateStatement;

    public AbstractRemoteCachedDataMapper(String serverUrl, int serverPort) {
        super(serverUrl, serverPort);
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setupDatabase() {
        try {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("create table if not exists items(name text primary key, object text, lastUpdate bigint)");

            insertStatement = connection.prepareStatement("insert into items values(?, ?, ?)");
            retrieveStatement = connection.prepareStatement("select name, object, lastUpdate from items where name = ?");
            deleteStatement = connection.prepareStatement("delete from items where name = ?");
            updateStatement = connection.prepareStatement("update items set object = ?, lastUpdate = ? where name = ?");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }


    @Override
    public boolean put(String itemName, Object obj) {
        try {
            long currentTime = getCurrentTime();
            boolean remotePut = super.put(itemName, obj);

            if (!remotePut) {
                return false;
            }

            return insertLocal(itemName, gson.toJson(obj), currentTime);

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public String getString(String item) {
        try {
            long currentTime = getCurrentTime();

            String obj = null;

            // is it in the cache?
            retrieveStatement.setString(1, item);
            ResultSet result = retrieveStatement.executeQuery();

            boolean hasResult = result.next();

            if (hasResult) {
                long lastUpdate = result.getLong("lastUpdate");

                if ((currentTime - lastUpdate) > outdated_delta) {
                    return result.getString("object");
                }

                obj = result.getString("object");
            }

            String remoteObj = super.getString(item);

            if (hasResult) {
                updateStatement.setString(1, remoteObj);
                updateStatement.setLong(2, currentTime);
                updateStatement.setString(3, item);
            } else {
                insertLocal(item, remoteObj, currentTime);
            }

            return remoteObj;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public Object get(String item, Class<?> klass) {
        return gson.fromJson(getString(item), klass);
    }

    public boolean delete(String item) {
        boolean remoteDelete = super.delete(item);

        if (!remoteDelete) {
            return false;
        }

        try {
            deleteStatement.setString(1, item);
            return deleteStatement.executeUpdate() == 1;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private boolean insertLocal(String itemName, String obj, long currentTime) {
        try {
            insertStatement.setString(1, itemName);
            insertStatement.setString(2, obj);
            insertStatement.setLong(3, currentTime);

            return insertStatement.executeUpdate() == 1;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public Map<String, String> getAll() {
        try {
            long currentTime = getCurrentTime();

            Statement statement = connection.createStatement();
            ResultSet query = statement.executeQuery("select * from items");

            Map<String, String> results = new HashMap<>();
            while (query.next()) {
                if ((currentTime - query.getLong("lastUpdate")) > outdated_delta) {
                    query.deleteRow();
                    continue;
                }

                results.put(query.getString("name"), query.getString("object"));
            }

            return results;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }


    private static long getCurrentTime() {
        return Calendar.getInstance().getTimeInMillis();
    }
}
