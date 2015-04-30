package co.kuntz.sqliteEngine.core;

import httpserver.*;
import com.google.gson.Gson;
import java.net.URLDecoder;

public class RemoteDataMapperServer extends HttpServer {
    public static final int DEFAULT_PORT = 8081;
    public static final String DEFAULT_NAME = "remote.db";

    private DataMapper dataMapper;
    private Gson gson = new Gson();

    public RemoteDataMapperServer() {
        this(DEFAULT_NAME, DEFAULT_PORT);
    }

    public RemoteDataMapperServer(String name) {
        this(name, DEFAULT_PORT);
    }

    public RemoteDataMapperServer(String name, int port) {
        super(port, "Remote Data Mapper Server", "0.0.1", "");

        dataMapper = LocalDataMapper.getDataMapperFactory(name).createMapper();

        get(new Route("/get/{item}") {
            @Override public void handle(HttpRequest request, HttpResponse response) {
                String item = dataMapper.getString(request.getParam("item"));
                if (item == null) {
                    item = "null";
                }
                response.setBody(item);
            }
        });

        post(new Route("/put/{item}") {
            @Override public void handle(HttpRequest request, HttpResponse response) {
                try {
                    String itemName = request.getParam("item");
                    String obj = URLDecoder.decode(request.getParam("object"), "UTF-8");
                    boolean added = ((LocalDataMapper) dataMapper).putRaw(itemName, obj);

                    response.setBody(gson.toJson(added));
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        });

        delete(new Route("/delete/{item}") {
            @Override public void handle(HttpRequest request, HttpResponse response) {
                boolean deleted = dataMapper.delete(request.getParam("item"));
                response.setBody(gson.toJson(deleted));
            }
        });

        get(new Route("/getall") {
            @Override public void handle(HttpRequest request, HttpResponse response) {
                response.setBody(gson.toJson(dataMapper.getAll()));
            }
        });
    }

    public void purgeData() {
        ((LocalDataMapper) dataMapper).purge();
    }
}
