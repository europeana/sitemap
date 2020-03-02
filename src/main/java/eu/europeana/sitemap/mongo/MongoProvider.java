package eu.europeana.sitemap.mongo;

import com.mongodb.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Connects to the (production) mongo server to retrieve all records.
 *
 * Created by ymamakis on 11/16/15.
 */
public class MongoProvider {

    private static final Logger LOG = LogManager.getLogger(MongoProvider.class);

    private MongoClient mongoClient;
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
        if (StringUtils.isEmpty(database)) {
            database = uri.getDatabase();
        }
        LOG.info("Connecting to Mongo {} database at {}...", database, uri.getHosts());
        this.mongoClient = new MongoClient(uri);
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
