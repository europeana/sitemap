package eu.europeana.sitemap.mongo;

import com.mongodb.*;
import org.apache.commons.lang.*;
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
    public MongoProvider(String mongoHosts, String port, String username, String password, String database) {

        String[] addresses = mongoHosts.split(",");
        List<ServerAddress> mongoAddresses = new ArrayList<>();
        try {
            for (String address : addresses) {
                ServerAddress mongoAddress = new ServerAddress(address, Integer.parseInt(port));
                mongoAddresses.add(mongoAddress);
            }
            if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                MongoCredential credential = MongoCredential.createCredential(username, database, password.toCharArray());
                List<MongoCredential> credentials = new ArrayList<>();
                credentials.add(credential);
                this.mongoClient = new MongoClient(mongoAddresses, credentials);
            } else {
                this.mongoClient = new MongoClient(mongoAddresses);
            }
            LOG.info("Connected to Mongo at "+mongoAddresses);

            this.collection = this.mongoClient.getDB(database).getCollection("record");
        } catch (UnknownHostException e) {
            LOG.error("Error connecting to Mongo server "+mongoAddresses, e);
        }
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
