package eu.europeana.sitemap.mongo;

import com.mongodb.*;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ymamakis on 11/16/15.
 */
public class MongoProvider {

    private DBCollection collection;

    public MongoProvider(String mongoHosts, String port, String username, String password, String database) {

        String[] addresses = mongoHosts.split(",");
        List<ServerAddress> mongoAddresses = new ArrayList<>();
        try {
            for (String address : addresses) {
                ServerAddress mongoAddress = new ServerAddress(address, Integer.parseInt(port));
                mongoAddresses.add(mongoAddress);

            }
            MongoCredential credential = MongoCredential.createCredential(username, database, password.toCharArray());
            List<MongoCredential> credentials = new ArrayList<>();
            credentials.add(credential);
            MongoClient mongoClient = new MongoClient(mongoAddresses, credentials);

            collection = mongoClient.getDB(database).getCollection("record");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public DBCollection getCollection() {
        return collection;
    }
}
