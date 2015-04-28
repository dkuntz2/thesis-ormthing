package co.kuntz.sqliteEngine.core;

import httpserver.*;
import com.google.gson.Gson;
import java.net.URLDecoder;

public class RemoteDataMapperServer extends HttpServer {
    public static final int DEFAULT_PORT = 8081;
    public static final String DEFAULT_NAME = "remote.db";

    private DataMapper dataMapper;

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
                response.setBody(item);
            }
        });

        post(new Route("/put/{item}") {
            @Override public void handle(HttpRequest request, HttpResponse response) {
                String itemName = request.getParam("item");
                String obj = URLDecoder.decode(request.getParam("object"));
                ((LocalDataMapper) dataMapper).putRaw(itemName, obj);

                response.noContent();
            }
        });

        delete(new Route("/delete/{item}") {
            @Override public void handle(HttpRequest request, HttpResponse response) {
                dataMapper.delete(request.getParam("item"));
                response.noContent();
            }
        });

        get(new Route("/getall") {
            @Override public void handle(HttpRequest request, HttpResponse response) {
                Gson gson = new Gson();
                response.setBody(gson.toJson(dataMapper.getAll()));
            }
        });
    }

    public void purgeData() {
        ((LocalDataMapper) dataMapper).purge();
    }
}
