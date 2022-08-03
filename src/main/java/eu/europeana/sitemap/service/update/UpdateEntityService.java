package eu.europeana.sitemap.service.update;


import com.jayway.jsonpath.JsonPath;
import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.SitemapType;
import eu.europeana.sitemap.config.PortalUrl;
import eu.europeana.sitemap.config.SitemapConfiguration;
import eu.europeana.sitemap.exceptions.EntityQueryException;
import eu.europeana.sitemap.exceptions.InvalidApiKeyException;
import eu.europeana.sitemap.exceptions.SiteMapException;
import eu.europeana.sitemap.service.ActiveDeploymentService;
import eu.europeana.sitemap.service.MailService;
import eu.europeana.sitemap.service.ReadSitemapService;
import eu.europeana.sitemap.service.ResubmitService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

/**
 * Service for updating the entity sitemap. This class gathers all the relevant entity data to add it to the sitemap
 * The rest of the update process is handled by the underlying abstract service.
 *
 * @author Patrick Ehlert
 * Created on 01-02-2019
 */
@Service
public class UpdateEntityService extends UpdateAbstractService {

    private static final Logger LOG = LogManager.getLogger(UpdateEntityService.class);

    private static final int ITEMS_PER_SITEMAP_FILE = 15_000;

    private static final int ENTITY_QUERY_PAGE_SIZE = 100;
    private static final String ENTITY_QUERY = "*&scope=europeana&type=agent,concept,timespan,organization&fl=id,type" + //,skos_prefLabel.*"
            "&pageSize=" +ENTITY_QUERY_PAGE_SIZE;

    private SitemapConfiguration config;
    private PortalUrl portalUrl;

    private CloseableHttpClient httpClient = HttpClients.createDefault();

    @Autowired
    public UpdateEntityService(SitemapConfiguration config, ObjectStorageClient objectStorage, ActiveDeploymentService deploymentService,
                               ReadSitemapService readSitemapService, ResubmitService resubmitService, MailService mailService,
                               PortalUrl portalUrl) {
        super(SitemapType.ENTITY, objectStorage, deploymentService, readSitemapService, resubmitService, mailService, ITEMS_PER_SITEMAP_FILE);
        this.config = config;
        this.portalUrl = portalUrl;
    }

    /**
     * Generate entity data (and save it with sitemapGenerator.addItem() method)
     * Never call this manually! It is automatically called by the UpdateAbstractService
     */
    protected void generate(SitemapGenerator sitemapGenerator) throws SiteMapException {
        long pageNr = 0;
        long retrieved = 0;
        long totalEntities = -1; // we update this value after first request (and check after each new page retrieval)

        LOG.info("Retrieving entity data...");
        while (retrieved < totalEntities || totalEntities < 0) {
            String entityData = this.getEntityJson(config.getEntityApi(), ENTITY_QUERY, pageNr, config.getEntityApiKey());

            EntityData[] entities = this.parseEntityData(entityData);
            for (EntityData entity : entities) {
                LOG.debug("Adding entity {} with type {}", entity.getId(), entity.getType());
                String url = portalUrl.getEntityUrl("en", entity.getType(), entity.getId(), null);
                sitemapGenerator.addItem(url, null, null); // there's no priority or lastmodified for entities
            }
            retrieved = retrieved + entities.length;
            long newTotalEntities = this.getTotalEntitiesCount(entityData);
            if (totalEntities > 0 && newTotalEntities != totalEntities) {
                LOG.warn("Total number of entities has changed during update! Not all entities may be listed");
            }
            totalEntities = newTotalEntities;
            pageNr++;
        }

    }

    @Override
    public String getWebsiteBaseUrl() {
        return config.getPortalBaseUrl();
    }

    /**
     * @see UpdateService#getUpdateInterval()
     */
    @Override
    public String getUpdateInterval() {
        return config.getEntityUpdateInterval();
    }

    /**
     * @see UpdateService#doResubmit()
     */
    @Override
    public boolean doResubmit() {
        return config.isEntityResubmit();
    }

    /**
     * Send query to Entity API and retrieve data
     */
    private String getEntityJson(URL entityApi, String query, long pageNr, String wsKey) throws SiteMapException {
        String result= null;

        StringBuilder request = new StringBuilder(entityApi.toString());
        request.append("?query=");
        request.append(query);
        request.append("&page=");
        request.append(pageNr);
        request.append("&wskey=");
        request.append(wsKey);

        try {
            String requestUrl = request.toString();
            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl))) {
                int responseCode = response.getStatusLine().getStatusCode();
                LOG.debug("Entity query: {}, status code = {}", request, responseCode);
                if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                    throw new InvalidApiKeyException("API key is not valid");
                } else if (responseCode != HttpStatus.SC_OK) {
                    throw new EntityQueryException("Error retrieving entity data: "
                            +response.getStatusLine().getReasonPhrase());
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity);
                    LOG.debug("Response = {}", result);
                    EntityUtils.consume(entity); // make sure entity is consumed fully so connection can be reused
                } else {
                    LOG.warn("Reponse = null");
                }
            }
        } catch (IOException e) {
            throw new EntityQueryException("Error retrieving entity data", e);
        }

        return result;
    }

    private long getTotalEntitiesCount(String entityJson) {
        return JsonPath.parse(entityJson).read("$.partOf.total", Long.class);
    }

    private EntityData[]  parseEntityData(String entityJson) {
        return JsonPath.parse(entityJson).read("$.items[*]", EntityData[].class);
    }

    /**
     * Class that contains all entity fields we specified in the query
     */
    public static class EntityData extends HashMap<String, Object> {

        private static final long serialVersionUID = 9157945603571553860L;

        /**
         * @return the entity id (url with data.europeana.eu as FQDN)
         */
        public String getId() {
            return (String) this.get("id");
        }

        /**
         * @return the type of Entity (agent or concept)
         */
        public String getType() {
            return (String) this.get("type");
        }
    }
}
