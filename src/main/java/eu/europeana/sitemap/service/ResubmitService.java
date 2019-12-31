package eu.europeana.sitemap.service;

import eu.europeana.sitemap.config.PortalUrl;
import eu.europeana.sitemap.SitemapType;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Service that resubmits our sitemap to search engines (when an update of the sitemap finishes).
 * Hopefully this prevents/reduces search engines from trying to access sitemap pages that do not exist anymore.
 *
 * See also https://support.google.com/webmasters/answer/183669?hl=en and
 * https://www.bing.com/webmaster/help/how-to-submit-sitemaps-82a15bd4
 * @author Patrick Ehlert on 11-9-17.
 */
@Service
public class ResubmitService {

    private static final Logger LOG = LogManager.getLogger(ResubmitService.class);

    private static final HttpClient HTTP_CLIENT = HttpClientBuilder.create().build();

    private PortalUrl portalUrl;

    @Autowired
    public ResubmitService(PortalUrl portalUrl) {
        this.portalUrl = portalUrl;
    }

    /**
     * Notify Google and Bing that our sitemap has changed (but only if the provided portal sitemap index url is not empty)
     */
    public void notifySearchEngines(SitemapType sitemapType) {
        try {
            // check if uri is valid
            URI sitemapFile = new URIBuilder(portalUrl.getSitemapIndexUrl(sitemapType)).build();
            LOG.info("Notifying search engines that {} sitemap is updated...", sitemapType);
            resubmitToServices(sitemapFile);
        } catch (URISyntaxException e) {
            LOG.error("No valid {} sitemap index url", sitemapType, e);
        }
    }

    private void resubmitToServices(URI sitemapFile) {
        try {
            resubmitToService("Google", "http://google.com/ping", "sitemap", sitemapFile);
            resubmitToService("Bing", "http://www.bing.com/ping", "sitemap", sitemapFile);
        } catch (URISyntaxException | IOException e) {
            LOG.error("Error pinging service", e);
        }
    }

    /**
     * Both Google and Bing only require a get-request as notification.
     * @param serviceName name of search engine
     * @param serviceUrl url where to submit updates index (which search engine)
     * @param serviceProperty property that is updated (normally always 'sitemap')
     * @throws URISyntaxException when the provided serviceUrl is not valid
     * @throws IOException when there is a problem sending the ping request
     */
    private void resubmitToService(String serviceName, String serviceUrl, String serviceProperty, URI sitemapFile) throws URISyntaxException, IOException {
        LOG.info("Pinging {} with index = {}", serviceName, sitemapFile.toString());
        URIBuilder uriBuilder = new URIBuilder(serviceUrl);
        uriBuilder.setParameter(serviceProperty, sitemapFile.toString());
        HttpGet getRequest = new HttpGet(uriBuilder.build());
        if (LOG.isDebugEnabled()) {
            LOG.debug("request = {} ", getRequest.getURI());
        }

        HttpResponse response = HTTP_CLIENT.execute(getRequest);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_OK) {
            LOG.info("{} says OK: {} ", serviceName, EntityUtils.toString(response.getEntity()));
        } else {
            LOG.error("{} says {} (status code {})", serviceName, response.getStatusLine().getReasonPhrase(), statusCode);
        }
    }


}
