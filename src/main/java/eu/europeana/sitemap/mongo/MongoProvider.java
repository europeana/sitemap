package eu.europeana.sitemap.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

/**
 * Connects to the (production) mongo server to retrieve all records.
 *
 * Created by ymamakis on 11/16/15.
 */
public class MongoProvider {

    private static final Logger LOG = LogManager.getLogger(MongoProvider.class);

    private final MongoClient mongoClient;
    private final MongoCollection<Document> collection;

    /**
     * Create a new MongoClient based on a connectionUrl
     *
     * @param connectionUrl url with the authentication database
     * @param database database to use (should be filled when used record database is different from authentication database)
     */
    public MongoProvider(String connectionUrl, String database) {
        ConnectionString connection = new ConnectionString(connectionUrl);
        if (StringUtils.isEmpty(database)) {
            database = connection.getDatabase();
        }
        LOG.info("Connecting to Mongo {} database at {}...", database, connection.getHosts());
        this.mongoClient = MongoClients.create(connection);
        this.collection = this.mongoClient.getDatabase(database).getCollection("record");
        LOG.info("Mongo record collection retrieved.");
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
    public MongoCollection<Document> getCollection() {
        return collection;
    }
}
