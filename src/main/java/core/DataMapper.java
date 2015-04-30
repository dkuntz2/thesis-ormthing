package co.kuntz.sqliteEngine.core;

import java.util.Map;

/**
 * Our goal tonight is to fuck with SQLite as much as possible, mainly because
 * it's the only thing you can use on Android without a huge hassle, but I want
 * something _super_ simple and relatively non-relational.
 *
 * So, how we gunna do this? Well, I'm thinking by pure force of will, or maybe
 * something a little more concrete.
 *
 * Please note, all of these notes are being written before, or during, the
 * writing of this code/library/package, and are meant to be a roadmap for where
 * I want to take this code.
 *
 * This means that if you're reading this (THAT INCLUDES YOU, FUTURE DON), don't
 * take any comments for granted, and assume they're not worth shit.
 *
 * ---------
 *
 * Anyways, where were we?
 *
 * Making this thing work:
 *      ONE (!) table: items:
 *          create table items(name text primary key, object text)
 *
 * DataMapper.insert("item name", item in json)
 *      item in json will probably just accept anything, or it might force the
 *      user to preconvert it to JSON, I'm not sure which I want yet... We'll
 *      see based on how easy it is to do later.
 */

public interface DataMapper {
    // TODO: convert insert/delete to booleans?
    public void put(String itemName, Object obj);
    public Object get(String itemName, Class<?> klass);
    public String getString(String itemName);
    public void delete(String itemName);
    public Map<String, String> getAll();

    public interface DataMapperFactory {
        public DataMapper createMapper();
    }
}
