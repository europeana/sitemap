package eu.europeana.sitemap.service.update;


import com.jayway.jsonpath.JsonPath;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.exceptions.EntityQueryException;
import eu.europeana.sitemap.exceptions.InvalidApiKeyException;
import eu.europeana.sitemap.exceptions.SiteMapConfigException;
import eu.europeana.sitemap.exceptions.SiteMapException;
import eu.europeana.sitemap.service.ActiveDeploymentService;
import eu.europeana.sitemap.service.ReadSitemapService;
import eu.europeana.sitemap.service.ResubmitService;
import eu.europeana.sitemap.SitemapType;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

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

    private static final int ITEMS_PER_SITEMAP_FILE = 20_000;

    private static final int ENTITY_QUERY_PAGE_SIZE = 100;
    private static final String ENTITY_QUERY = "*&scope=europeana&type=agent,concept&fl=id" + //,type,skos_prefLabel.*"
            "&pageSize=" +ENTITY_QUERY_PAGE_SIZE;

    @Value("${portal.base.url}")
    private String portalBaseUrl;

    @Value("${entity.portal.urlpath}")
    private String portalPath;

    @Value("${entity.api.url}")
    private String entityApiUrl;
    protected URL entityApi;
    @Value("${entity.api.wskey}")
    protected String entityApiKey;

    private CloseableHttpClient httpClient = HttpClients.createDefault();

    @Autowired
    public UpdateEntityService(ObjectStorageClient objectStorage, ActiveDeploymentService deploymentService,
                               ReadSitemapService readSitemapService, ResubmitService resubmitService) {
        super(SitemapType.ENTITY, objectStorage, deploymentService, readSitemapService, resubmitService, ITEMS_PER_SITEMAP_FILE);
    }

    @PostConstruct
    private void init() throws SiteMapConfigException {
        // check configuration for required properties
        if (StringUtils.isEmpty(portalBaseUrl)) {
            throw new SiteMapConfigException("Portal.base.url is not set");
        }
        if (StringUtils.isEmpty(portalPath)) {
            throw new SiteMapConfigException("Property entity.portal.urlpath is not set");
        }
        if (StringUtils.isEmpty(entityApiUrl)) {
            throw new SiteMapConfigException("Property entity.api.url is not set");
        }
        try {
            this.entityApi = new URL(entityApiUrl);
        } catch (MalformedURLException e) {
            throw new SiteMapConfigException("Property entity.api.url is incorrect: "+entityApiUrl);
        }

        // trim to avoid problems with accidental trailing spaces
        portalBaseUrl = portalBaseUrl.trim();
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
            String entityData = this.getEntityJson(entityApi, ENTITY_QUERY, pageNr, entityApiKey);

            List<String> entityIds = this.getEntityIds(entityData);
            for (String entityId : entityIds) {
                LOG.debug("Adding entity {}", entityId);
                sitemapGenerator.addItem(entityId, null, null);
            }
            retrieved = retrieved + entityIds.size();
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
        return portalBaseUrl + portalPath;
    }


    /**
     * Send query to Entity API and retrieve data
     */
    @HystrixCommand(ignoreExceptions = {EntityQueryException.class}, commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "20000"),
            @HystrixProperty(name = "fallback.enabled", value="false")
    })
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

    private List<String> getEntityIds(String entityJson) {
        return JsonPath.parse(entityJson).read("$.items[*].id", List.class);
    }

}
