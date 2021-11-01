package eu.europeana.sitemap.service.update;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import eu.europeana.sitemap.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Utils class for updating records
 *
 * @author Srishti Singh on 31 October 2021
 */
public class UpdateRecordServiceUtils {

    private static final Logger LOG = LogManager.getLogger(UpdateRecordServiceUtils.class);


    private UpdateRecordServiceUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * calculate sitemap priority based on Tiers
     * @param contentTier
     * @param metadatTier
     * @return String
     */
    // TODO discuss the piority for contentTier and metadataTier
    public static String getPriorityForTiers(int contentTier, String metadatTier) {
        if (contentTier > 3 && ( metadatTier.equals(Constants.METADATA_TIER_A) || metadatTier.equals(Constants.METADATA_TIER_B))) {
          return "1.0";
      }
      return  "0.9";
    }

    /**
     * Calculate sitemap priority based on europeanaCompleteness
     * @param completeness europeanaCompleteness
     * @return String
     */
    public static String getPriorityForCompleteness(int completeness) {
       return (completeness > 9 ? "1.0" : ("0." + completeness));
    }

    public static DBObject getQuery(int recordCompleteness) {
        if (recordCompleteness  >= 0) {
            LOG.info("Filtering records based on Europeana Completeness score of at least {}", recordCompleteness);
            return new BasicDBObject(Constants.COMPLETENESS, new BasicDBObject(Constants.GTE, recordCompleteness) );
        }
        return new BasicDBObject();
    }

    /**
     * Returns the aggregation pipeline
     * @param contentTier contentTier values to be included
     * @param metadataTier metadataTier values to be included
     * @return List<BasicDBObject>
     */
    public static List<BasicDBObject> getPipeline(String contentTier, String metadataTier) {
        // fetches the value of contentTier and metaDataTier present in #qualityAnnotation.body
        // ex: contentTierUrl:"http://www.europeana.eu/schemas/epf/contentTier2" , metadataTierUrl:"http://www.europeana.eu/schemas/epf/metadataTier0
        BasicDBObject getTiersIndividually = new BasicDBObject(Constants.PROJECT,
                getCommonProjections()
                        .append("contentTierValue",
                                new BasicDBObject(Constants.ARRAY_ELEMENT_AT, Arrays.asList(Constants.MONGO_QA_BODY, 0L)))
                        .append("metadataTierValue",
                                new BasicDBObject(Constants.ARRAY_ELEMENT_AT, Arrays.asList(Constants.MONGO_QA_BODY, -1L))));

        // extracts the value from the urls fetched previously. ex: contentTier:"2" , metadataTier:"0"
        BasicDBObject getTierValues = new BasicDBObject(Constants.PROJECT,
                getCommonProjections()
                        .append(Constants.CONTENT_TIER,
                                new BasicDBObject(Constants.ARRAY_ELEMENT_AT, Arrays.asList(
                                        new BasicDBObject(Constants.SPLIT, Arrays.asList("$contentTierValue", Constants.CONTENT_TIER)), -1L)))
                        .append(Constants.METADATA_TIER,
                                new BasicDBObject(Constants.ARRAY_ELEMENT_AT, Arrays.asList(
                                        new BasicDBObject(Constants.SPLIT, Arrays.asList("$metadataTierValue", Constants.METADATA_TIER)), -1L))));

        BasicDBObject matchCriteria = getMatchCriteria(contentTier, metadataTier);
        // if content tier and metadataTier are empty, will just return the basic values without filtering
        // ex: { about:"test", europeanaCompleteness: 2, timestampUpdated:2016-04-30T17:57:10.744+00:00, contentTier:"2", metadataTier:"A"}
        // This is to avoid any Mongo aggregation exception
        if (matchCriteria.isEmpty()) {
            return Arrays.asList(getTiersIndividually, getTierValues);
        }
        return Arrays.asList(getTiersIndividually, getTierValues, matchCriteria);
    }

    /**
     * Returns the basic fields for projection
     * @return
     */
    public static BasicDBObject getCommonProjections(){
        return new BasicDBObject(Constants.ABOUT, 1L)
                .append(Constants.LASTUPDATED, 1L)
                .append(Constants.COMPLETENESS, 1L);
    }

    /**
     * Returns the $match filter
     * {$match: { $and: [{contentTier: {$in: [<contentTier>]}},
     *                   {metadataTier: {$in: [<metadataTier>>]}}]}}
     * NOTE : Both contentTier and metadataTier values are String.
     *
     * @param contentTier
     * @param metadataTier
     * @return
     */
    private static BasicDBObject getMatchCriteria(String contentTier, String metadataTier) {
        List<BasicDBObject> andStages = new ArrayList<>();
        if (!contentTier.isEmpty()) {
            LOG.info("Filtering records based on Europeana content Tier {}", contentTier);
            List<String> values = List.of(contentTier.split(","));
            andStages.add(new BasicDBObject(Constants.CONTENT_TIER,
                    new BasicDBObject(Constants.IN, values)));
        }
        if (!metadataTier.isEmpty()) {
            LOG.info("Filtering records based on Europeana metaData Tier {}", metadataTier.toUpperCase(Locale.ROOT));
            List<String> values = List.of(metadataTier.toUpperCase(Locale.ROOT).split(","));
            andStages.add(new BasicDBObject(Constants.METADATA_TIER,
                    new BasicDBObject(Constants.IN, values)));
        }
        if (!andStages.isEmpty()) {
            return new BasicDBObject(Constants.MATCH,
                    new BasicDBObject(Constants.AND, andStages));
        }
        return new BasicDBObject();
    }
}
