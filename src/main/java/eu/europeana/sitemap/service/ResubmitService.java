package eu.europeana.sitemap.service;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Service that resubmits our sitemap to search engines (when an update of the sitemap finishes).
 * Hopefully this prevents search engines from trying to access sitemap pages that do not exist anymore.
 *
 * See also https://support.google.com/webmasters/answer/183669?hl=en and
 * https://www.bing.com/webmaster/help/how-to-submit-sitemaps-82a15bd4
 * @author Patrick Ehlert on 11-9-17.
 */
@Service
public class ResubmitService {

    private static final Logger LOG = LoggerFactory.getLogger(ResubmitService.class);

    private static final HttpClient HTTP_CLIENT = HttpClientBuilder.create().build();

    @Value("#{sitemapProperties['index.deploy.url']}")
    private String indexUrl;

    /**
     * Notify Google and Bing that our sitemap has changed
     */
    public void notifySearchEngines() {
        if (StringUtils.isNotEmpty(indexUrl)) {
            try {
                // check if uri is valid
                URI sitemapFile = new URIBuilder(indexUrl).build();
                LOG.info("Notifying search engines that sitemap is updated...");
                try {
                    resubmitToService("Google", "http://google.com/ping", "sitemap", sitemapFile);
                    resubmitToService("Bing", "http://www.bing.com/ping", "sitemap", sitemapFile);
                } catch (URISyntaxException | IOException e) {
                    LOG.error("Error pinging service", e);
                }
            } catch (URISyntaxException e) {
                LOG.error("No valid sitemap index url specified", e);
            }
        } else {
            LOG.info("No sitemap index url specified, skipping search engine notification");
        }
    }

    /**
     * Both Google and Bing only require a get-request as notification.
     * @param serviceName
     * @param serviceUrl
     * @param serviceProperty
     * @throws URISyntaxException
     * @throws IOException
     */
    private void resubmitToService(String serviceName, String serviceUrl, String serviceProperty, URI sitemapFile) throws URISyntaxException, IOException {
        LOG.info("Pinging {} with index = {}", serviceName, indexUrl);
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

    /**
     * To manually notify search engines we can run this as a standalone java program
     * @param args
     */
    public static void main(String[] args) {
        ResubmitService ss = new ResubmitService();
        ss.indexUrl = "http://www.europeana.eu/portal/europeana-sitemap-index-hashed.xml";
        ss.notifySearchEngines();
    }

}
