package eu.europeana.sitemap.mongo;

import com.mongodb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Connects to the (production) mongo server to retrieve all records.
 *
 * Created by ymamakis on 11/16/15.
 */
public class MongoProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MongoProvider.class);

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
            LOG.info("Connected to Mongo at "+mongoAddresses);

            collection = mongoClient.getDB(database).getCollection("record");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public DBCollection getCollection() {
        return collection;
    }
}
