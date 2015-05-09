package co.kuntz.sqliteEngine.core;

import java.util.Map;

/**
 * A DataMapper is an interface for a key/value data store.
 */
public interface DataMapper {
    public boolean put(String itemName, Object obj);

    public Object get(String itemName, Class<?> klass);
    public String getString(String itemName);

    public Map<String, Object> startsWith(String prefix, Class<?> klass);
    public Map<String, String> startsWithRaw(String prefix);

    public boolean delete(String itemName);

    public Map<String, String> getAll();

}
