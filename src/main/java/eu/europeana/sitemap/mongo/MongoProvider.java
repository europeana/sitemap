package eu.europeana.sitemap.mongo;

import com.mongodb.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Connects to the (production) mongo server to retrieve all records.
 *
 * Created by ymamakis on 11/16/15.
 */
public class MongoProvider {

    private static final Logger LOG = LogManager.getLogger(MongoProvider.class);

    private MongoClient mongoClient;
    private String database;
    private DBCollection collection;

    // TODO replace deprecated getDb() record retrieval with more up-to-date method

    /**
     * Create a new MongoClient based on a connectionUrl, e.g.
     * mongodb://user:password@mongo1.eanadev.org:27000/europeana_1
     * @see <a href="http://api.mongodb.com/java/current/com/mongodb/MongoClientURI.html">
     *     MongoClientURI documentation</a>
     *
     * @param connectionUrl url with the authentication database
     * @param database database to use (should be filled when used record database is different from authentication database)
     */
    public MongoProvider(String connectionUrl, String database) {
        MongoClientURI uri = new MongoClientURI(connectionUrl);
        this.database = database;
        if (StringUtils.isEmpty(database)) {
            database = uri.getDatabase();
        }
        LOG.info("Connecting to Mongo {} database at {}...", this.database, uri.getHosts());
        this.mongoClient = new MongoClient(uri);
        this.collection = this.mongoClient.getDB(database).getCollection("record");
        LOG.info("Mongo collection retrieved.");
    }

    /**
     * Setup a new connection to the Mongo database
     * @param mongoHosts list of mongo host names separated by comma
     * @param port port number of mongo host
     * @param authDatabase database name used for authentication
     * @param username username
     * @param password password
     * @param database database name used for retrieving record data
     */
    public MongoProvider(String mongoHosts, String port, String authDatabase, String username, String password, String database) {
        String[] addresses = mongoHosts.split(",");
        List<ServerAddress> mongoAddresses = new ArrayList<>(addresses.length);
        for (String address : addresses) {
            ServerAddress mongoAddress = new ServerAddress(address, Integer.parseInt(port));
            mongoAddresses.add(mongoAddress);
        }
        this.database = database;

        LOG.info("Connecting to Mongo {} database at {} ...", mongoAddresses, this.database);
        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            MongoCredential credential = MongoCredential.createCredential(username, authDatabase, password.toCharArray());
            List<MongoCredential> credentials = new ArrayList<>();
            credentials.add(credential);

            this.mongoClient = new MongoClient(mongoAddresses, credentials);
        } else {
            this.mongoClient = new MongoClient(mongoAddresses);
        }
        this.collection = this.mongoClient.getDB(database).getCollection("record");
        LOG.info("Mongo collection retrieved.");
    }

    /**
     * Close the connection to mongo
     */
    public void close() {
        if (mongoClient != null) {
            LOG.info("Shutting down connections to Mongo...");
            mongoClient.close();
        }
    }

    /**
     * @return Retrieve the entire record collection from our mongo database
     */
    public DBCollection getCollection() {
        return collection;
    }
}
