package co.kuntz.sqliteEngine.core;

import java.util.Map;
import java.util.HashMap;
import com.google.gson.Gson;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RemoteDataMapper extends DataMapper {
    private Gson gson = new Gson();
    private String serverAddr;

    enum RequestMethod {
        DELETE, GET, POST, PUT;
    }

    private RemoteDataMapper(String serverUrl, int serverPort) {
        serverAddr = serverUrl + ":" + serverPort + "/";
    }

    public static DataMapper.DataMapperFactory getDataMapperFactory(final String serverUrl, final int port) {
        return new DataMapper.DataMapperFactory() {
            @Override public DataMapper createMapper() {
                return new RemoteDataMapper(serverUrl, port);
            }
        };
    }

    public static DataMapper.DataMapperFactory getDataMapperFactory() {
        return getDataMapperFactory("http://localhost", RemoteDataMapperServer.DEFAULT_PORT);
    }

    private String remoteRequest(RequestMethod method, String address, Object obj) {
        try {
            URL url = new URL(serverAddr + address);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method.toString());

            if ((method.equals(RequestMethod.POST) || method.equals(RequestMethod.PUT)) && obj != null) {
                connection.setDoOutput(true);

                //String objString = URLEncoder.encode(gson.toJson(obj));
                String objString = gson.toJson(obj);
                OutputStream output = connection.getOutputStream();

                String outputStr = "object=" + objString;

                output.write(outputStr.getBytes("UTF-8"));
                output.close();
            }

            int respCode = connection.getResponseCode();
            if (respCode < 200 || respCode >= 300) {
                throw new RuntimeException("Bad request!");
            }

            InputStreamReader inputReader = new InputStreamReader(connection.getInputStream());
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[0x1000];
            int bytesRead = inputReader.read(buffer, 0, buffer.length);
            while (bytesRead >= 0) {
                builder.append(buffer, 0, bytesRead);
                bytesRead = inputReader.read(buffer, 0, buffer.length);
            }
            connection.disconnect();

            return builder.toString();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private String remoteRequest(RequestMethod method, String address) {
        return remoteRequest(method, address, null);
    }


    public void put(String itemName, Object obj) {
        remoteRequest(RequestMethod.POST, "put/" + itemName, obj);
    }

    public Object get(String item, Class<?> klass) {
        return gson.fromJson(getString(item), klass);
    }

    public String getString(String item) {
        return remoteRequest(RequestMethod.GET, "get/" + item);
    }

    public void delete(String item) {
        remoteRequest(RequestMethod.DELETE, "delete/" + item);
    }

    public Map<String, String> getAll() {
        String allString = remoteRequest(RequestMethod.GET, "getall");

        HashMap<String, String> all = new HashMap<String,String>();
        all = (HashMap<String, String>) gson.fromJson(allString, all.getClass());

        return all;
    }
}
