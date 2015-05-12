package co.kuntz.sqliteEngine.core;

import java.util.Map;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A RemoteDataMapper is a {@link DataMapper} which stores data on a separate
 * server, running a {@link RemoteDataMapperServer}.
 */
public class RemoteDataMapper implements DataMapper {
    private Gson gson = new Gson();
    private String serverAddr;

    enum RequestMethod {
        DELETE, GET, POST, PUT;
    }

    class Pair {
        public String key;
        public String value;

        public Pair(String k, String v) {
            this.key = k;
            this.value = v;
        }
    }

    public RemoteDataMapper(String serverUrl, int serverPort) {
        serverAddr = serverUrl + ":" + serverPort + "/";
    }

    public RemoteDataMapper() {
        this("http://localhost", RemoteDataMapperServer.DEFAULT_PORT);
    }


    private String remoteRequest(RequestMethod method, String address, Object obj) {
        try {
            String urlString = serverAddr + address;

            if (method.equals(RequestMethod.GET) && obj != null) {
                String objString = "?object=" + URLEncoder.encode(gson.toJson(obj), "UTF-8");
                urlString += objString;
            }

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method.toString());

            if ((method.equals(RequestMethod.POST) || method.equals(RequestMethod.PUT) || method.equals(RequestMethod.DELETE)) && obj != null) {
                connection.setDoOutput(true);

                String objString = gson.toJson(obj);
                OutputStream output = connection.getOutputStream();

                String outputStr = "object=" + objString;

                output.write(outputStr.getBytes("UTF-8"));
                output.close();
            }

            int respCode = connection.getResponseCode();
            if (respCode < 200 || respCode >= 300) {
                String response = readFromStream(connection.getErrorStream());
                connection.disconnect();

                throw new RuntimeException("Bad request! URL: \"" + urlString + "\", response code: " + respCode + " Response: " + response);
            }

            String response = readFromStream(connection.getInputStream());
            connection.disconnect();

            return response;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private String readFromStream(InputStream stream) throws Throwable {
        InputStreamReader inputReader = new InputStreamReader(stream);
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[0x1000];
        int bytesRead = inputReader.read(buffer, 0, buffer.length);
        while (bytesRead >= 0) {
            builder.append(buffer, 0, bytesRead);
            bytesRead = inputReader.read(buffer, 0, buffer.length);
        }

        return builder.toString();
    }

    private String remoteRequest(RequestMethod method, String address) {
        return remoteRequest(method, address, null);
    }


    public boolean put(String itemName, Object obj) {
        return Boolean.parseBoolean(remoteRequest(RequestMethod.POST, "put", new Pair(itemName, gson.toJson(obj))));
    }

    public Object get(String item, Class<?> klass) {
        return gson.fromJson(getString(item), klass);
    }

    public String getString(String item) {
        String obj = remoteRequest(RequestMethod.GET, "get", item);

        if (obj.equals("null")) {
            obj = null;
        }

        return obj;
    }

    public boolean delete(String item) {
        return Boolean.parseBoolean(remoteRequest(RequestMethod.DELETE, "delete", item));
    }

    public Map<String, String> getAll() {
        String allString = remoteRequest(RequestMethod.GET, "getall");

        Type hashMapType = new TypeToken<HashMap<String, String>>(){}.getType();
        HashMap<String, String> all = gson.fromJson(allString, hashMapType);

        return all;
    }

    public Map<String, String> startsWithRaw(String prefix) {
        String startsWithString = remoteRequest(RequestMethod.GET, "startsWith", prefix);

        Type hashMapType = new TypeToken<HashMap<String, String>>(){}.getType();
        HashMap<String, String> startsWith = gson.fromJson(startsWithString, hashMapType);

        return startsWith;
    }

    public Map<String, Object> startsWith(String prefix, Class<?> klass) {
        Map<String, String> raw = startsWithRaw(prefix);

        Map<String, Object> results = new HashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            results.put(entry.getKey(), gson.fromJson(entry.getValue(), klass));
        }

        return results;
    }
}
