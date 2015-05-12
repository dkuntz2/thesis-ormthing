package co.kuntz.sqliteEngine.core;

import httpserver.*;
import com.google.gson.Gson;
import java.net.URLDecoder;

/**
 * A RemoteDataMapperServer is a relatively simple HTTP server which can have
 * requests made of it to use a {@link DataMapper}. With a RemoteDataMapperServer
 * running on a separate machine, one can use a {@link RemoteDataMapper} to
 * access use it's data store.
 */
public class RemoteDataMapperServer extends HttpServer {
    public static final int DEFAULT_PORT = 8081;
    public static final String DEFAULT_NAME = "remote.db";

    private LocalDataMapper dataMapper;
    private Gson gson = new Gson();

    public RemoteDataMapperServer() {
        this(DEFAULT_NAME, DEFAULT_PORT);
    }

    public RemoteDataMapperServer(String name) {
        this(name, DEFAULT_PORT);
    }

    public RemoteDataMapperServer(String name, int port) {
        super(port, "Remote Data Mapper Server", "0.0.1", "");

        dataMapper = new LocalDataMapper(name);

        get(new Route("/get") {
            @Override public void handle(HttpRequest request, HttpResponse response) {
                try {
                    String rawName = URLDecoder.decode(request.getParam("object"), "UTF-8");
                    String itemName = gson.fromJson(rawName, String.class);
                    String item = dataMapper.getString(itemName);
                    if (item == null) {
                        item = "null";
                    }
                    response.setBody(item);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        });

        post(new Route("/put") {
            @Override public void handle(HttpRequest request, HttpResponse response) {
                try {
                    String rawPair = URLDecoder.decode(request.getParam("object"), "UTF-8");
                    RemoteDataMapper.Pair pair = gson.fromJson(rawPair, RemoteDataMapper.Pair.class);
                    boolean added = dataMapper.putRaw(pair.key, pair.value);

                    response.setBody(gson.toJson(added));
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        });

        delete(new Route("/delete") {
            @Override public void handle(HttpRequest request, HttpResponse response) {
                try {
                    String rawName = URLDecoder.decode(request.getParam("object"), "UTF-8");
                    String itemName = gson.fromJson(rawName, String.class);
                    boolean deleted = dataMapper.delete(itemName);
                    response.setBody(gson.toJson(deleted));
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        });

        get(new Route("/getall") {
            @Override public void handle(HttpRequest request, HttpResponse response) {
                response.setBody(gson.toJson(dataMapper.getAll()));
            }
        });

        get(new Route("/startsWith") {
            @Override public void handle(HttpRequest request, HttpResponse response) {
                try {
                    String obj = URLDecoder.decode(request.getParam("object"), "UTF-8");
                    String prefix = gson.fromJson(obj, String.class);

                    response.setBody(gson.toJson(dataMapper.startsWithRaw(prefix)));
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        });
    }
}
