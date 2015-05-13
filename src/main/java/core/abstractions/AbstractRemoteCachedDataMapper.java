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
    private PreparedStatement likeStatement;
    private PreparedStatement deleteStatement;
    private PreparedStatement updateStatement;
    private PreparedStatement prefixQueryStatement;

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
            stmt.executeUpdate("create table if not exists prefixQuery(prefix text primary key, lastUpdate bigint)");

            insertStatement = connection.prepareStatement("insert into items values(?, ?, ?)");
            retrieveStatement = connection.prepareStatement("select name, object, lastUpdate from items where name = ?");
            likeStatement = connection.prepareStatement("select name, object from items where name like ?");
            deleteStatement = connection.prepareStatement("delete from items where name = ?");
            updateStatement = connection.prepareStatement("update items set object = ?, lastUpdate = ? where name = ?");
            prefixQueryStatement = connection.prepareStatement("select prefix, lastUpdate from prefixQuery where prefix = ?");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }


    @Override
    public boolean put(String itemName, Object obj) {
        try {
            long currentTime = getCurrentTime();

            try {
                boolean remotePut = super.put(itemName, obj);

                if (!remotePut) {
                    return false;
                }
            } catch (Throwable t) {
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

            try {
                String remoteObj = super.getString(item);

                if (hasResult) {
                    updateLocal(item, remoteObj, currentTime);
                } else {
                    insertLocal(item, remoteObj, currentTime);
                }

                return remoteObj;
            } catch (Throwable t) {
                return obj;
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public Object get(String item, Class<?> klass) {
        return gson.fromJson(getString(item), klass);
    }

    public boolean delete(String item) {
        try {
            boolean remoteDelete = super.delete(item);

            if (!remoteDelete) {
                return false;
            }
        } catch (Throwable t) {
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

    private boolean updateLocal(String itemName, String obj, long currentTime) {
        try {
            updateStatement.setString(1, obj);
            updateStatement.setLong(2, currentTime);
            updateStatement.setString(3, itemName);

            return updateStatement.executeUpdate() == 1;
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

    @Override
    public Map<String, String> startsWithRaw(String prefix) {
        try {
            long currentTime = getCurrentTime();

            likeStatement.setString(1, prefix + "%");
            ResultSet localQuery = likeStatement.executeQuery();

            Map<String, String> results = new HashMap<>();
            while (localQuery.next()) {
                results.put(localQuery.getString("name"), localQuery.getString("object"));
            }

            prefixQueryStatement.setString(1, prefix);
            ResultSet query = prefixQueryStatement.executeQuery();

            boolean grabRemote = true;
            if (query.next()) {
                grabRemote = (currentTime - query.getLong("lastUpdate")) > outdated_delta;
            }

            if (grabRemote) {
                try {
                    Map<String, String> remoteResults = super.startsWithRaw(prefix);
                    for (Map.Entry<String, String> entry : remoteResults.entrySet()) {
                        String localVal = results.get(entry.getKey());

                        if (localVal == null) {
                            insertLocal(entry.getKey(), entry.getValue(), currentTime);
                            results.put(entry.getKey(), entry.getValue());
                        } else if (!localVal.equals(entry.getValue())) {
                            updateLocal(entry.getKey(), entry.getValue(), currentTime);
                            results.put(entry.getKey(), entry.getValue());
                        }
                    }
                } catch (Throwable t) {
                    // meh?
                }
            }

            return results;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public Map<String, Object> startsWith(String prefix, Class<?> klass) {
        Map<String, String> raw = startsWithRaw(prefix);

        Map<String, Object> results = new HashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            results.put(entry.getKey(), gson.fromJson(entry.getValue(), klass));
        }

        return results;
    }


    private static long getCurrentTime() {
        return Calendar.getInstance().getTimeInMillis();
    }
}
