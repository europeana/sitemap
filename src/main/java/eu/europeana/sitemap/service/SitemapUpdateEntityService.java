package eu.europeana.sitemap.service;


import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.FileNames;
import eu.europeana.sitemap.exceptions.SiteMapConfigException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 *
 */
//@Service
public class SitemapUpdateEntityService extends SitemapUpdateAbstractService {

    private static final Logger LOG = LogManager.getLogger(SitemapUpdateEntityService.class);

    private static final String FILE_NAME_BASE = FileNames.SITEMAP_ENTITY_FILENAME_BASE;

    public static final int NUMBER_OF_ELEMENTS = 45_000;

    private final ObjectStorageClient objectStorageProvider;
    private final ReadSitemapService readSitemapService;
    private final ResubmitService resubmitService;

    @Value("${portal.base.url}")
    private String portalBaseUrl;
    @Value("${entity.portal.urlpath}")
    private String portalEntityUrlPath;

    private String status = "initial";
    private Date updateStartTime;

    public SitemapUpdateEntityService(ObjectStorageClient objectStorageProvider, ReadSitemapService readSitemapService,
                                      ResubmitService resubmitService) {
        super(SitemapType.ENTITY, readSitemapService, resubmitService);
        LOG.info("init");
        this.objectStorageProvider = objectStorageProvider;
        this.readSitemapService = readSitemapService;
        this.resubmitService = resubmitService;
    }

    @PostConstruct
    private void init() throws SiteMapConfigException {
        // check configuration for required properties
        if (StringUtils.isEmpty(portalBaseUrl)) {
            throw new SiteMapConfigException("Portal.base.url is not set");
        }
        // trim to avoid problems with accidental trailing spaces
        portalBaseUrl = portalBaseUrl.trim();

        if (StringUtils.isEmpty(portalEntityUrlPath)) {
            throw new SiteMapConfigException("Portal.entity.urlpath is not set");
        }
        portalEntityUrlPath = portalEntityUrlPath.trim();
    }

    /**
     * Delete the old sitemap files, generate new ones and switch blue/green deployment
     */
    public void generate() {
        // get entity data!

        ActiveSitemapService activeSitemapService = new ActiveSitemapService(objectStorageProvider, FILE_NAME_BASE);
        SitemapGenerator generator = new SitemapGenerator(objectStorageProvider, activeSitemapService.getInactiveFile(),
                portalBaseUrl, FILE_NAME_BASE, NUMBER_OF_ELEMENTS);
        activeSitemapService.deleteInactiveFiles();

//        while (cur.hasNext()) {

//            LOG.debug("Adding record {}, completeness = {}, updated = {}", about, completeness, dateUpdated);
//            generator.addItem(about, String.valueOf(completeness), dateUpdated);
    //    }
  //      cur.close();

        generator.finish();
        activeSitemapService.switchFile();
    }



}
