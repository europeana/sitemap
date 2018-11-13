package eu.europeana.sitemap.mongo;

import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Connects to the (production) mongo server to retrieve all records.
 *
 * Created by ymamakis on 11/16/15.
 */
@Component
public class MongoProvider {

    private static final Logger LOG = LogManager.getLogger(MongoProvider.class);

    private MongoClient mongoClient;
    private DBCollection collection;

    /**
     * Setup a new connection to the Mongo database
     * @param mongoHosts
     * @param port
     * @param username
     * @param password
     * @param database
     */
    public MongoProvider(String mongoHosts, String port, String authDatabase, String username, String password, String database) {
        String[] addresses = mongoHosts.split(",");
        List<ServerAddress> mongoAddresses = new ArrayList<>(addresses.length);
        for (String address : addresses) {
            ServerAddress mongoAddress = new ServerAddress(address, Integer.parseInt(port));
            mongoAddresses.add(mongoAddress);
        }
        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            MongoCredential credential = MongoCredential.createCredential(username, authDatabase, password.toCharArray());
            List<MongoCredential> credentials = new ArrayList<>();
            credentials.add(credential);
            this.mongoClient = new MongoClient(mongoAddresses, credentials);
        } else {
            this.mongoClient = new MongoClient(mongoAddresses);
        }
        LOG.info("Connected to Mongo at {} ", mongoAddresses);

        this.collection = this.mongoClient.getDB(database).getCollection("record");
    }

    /**
     * Close the connection to mongo
     */
    public void close() {
        LOG.info("Shutting down connections to Mongo...");
        mongoClient.close();
    }

    /**
     * @return Retrieve the entire record collection from our mongo database
     */
    public DBCollection getCollection() {
        return collection;
    }
}
