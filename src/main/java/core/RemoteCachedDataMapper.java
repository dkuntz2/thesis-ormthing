package co.kuntz.sqliteEngine.core;

import co.kuntz.sqliteEngine.core.abstractions.AbstractRemoteCachedDataMapper;

import java.sql.Connection;
import java.sql.DriverManager;

import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;

import com.google.gson.Gson;

public class RemoteCachedDataMapper extends AbstractRemoteCachedDataMapper {
    public static final String DEFAULT_DB_NAME = "cached.db";
    public static final long OUTDATED_CACHE_DELTA = TimeUnit.MINUTES.toMillis(30);

    public RemoteCachedDataMapper(String dbName) {
        this(dbName, "http://localhost", RemoteDataMapperServer.DEFAULT_PORT);
    }

    public RemoteCachedDataMapper(String serverUrl, int serverPort) {
        this(DEFAULT_DB_NAME, serverUrl, serverPort);
    }

    public RemoteCachedDataMapper(String dbName, String serverUrl, int serverPort) {
        super(serverUrl, serverPort);

        try {
            Class.forName("org.sqlite.JDBC");

            if (!dbName.equals(":memory:") && !dbName.endsWith(".db")) {
                dbName = dbName + ".db";
            }
            setConnection(DriverManager.getConnection("jdbc:sqlite:" + dbName));
            setupDatabase();

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
