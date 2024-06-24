package eu.europeana.sitemap.web;

import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.service.ReadSitemapService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generic functionality for reading sitemap files (for testing and debugging)
 *
 * @author Patrick Ehlert
 * Created on 30-05-2018
 */
@RestController
@RequestMapping(value = "/")
public class SitemapFileController {

    private static final Logger LOG = LogManager.getLogger(SitemapFileController.class);

    private static final String FILENAME_REGEX = "^[a-zA-Z0-9_=&\\-\\.\\?]*$";
    private static final String INVALID_FILENAME_MSG = "Illegal file name";

    protected final ReadSitemapService service;

    @Autowired
    public SitemapFileController(ReadSitemapService service) {
        this.service = service;
    }

    /**
     * Lists all files stored in the used bucket (for debugging purposes)
     * @return list of all files in the bucket
     */
    @GetMapping(value = {"list", "files"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public String files() {
        return service.getFiles();
    }

    /**
     * Returns the contents of a particular file (in text/plain format)
     * @param fileName name of the requested file
     * @return contents of the requested file
     * @throws SiteMapNotFoundException when the requested file is not found
     */
    @GetMapping(value = "file.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<InputStreamResource> fileTxt(@RequestParam(value = "name", defaultValue = "")
                              @Pattern(regexp = FILENAME_REGEX, message = INVALID_FILENAME_MSG) String fileName) throws SiteMapNotFoundException {
        LOG.debug("Retrieving text file {} ", fileName);
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Please provide a file name");
        }
        InputStreamResource result = new InputStreamResource(service.getFileAsStream(fileName));
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(result); // if we don't set content-type gzip will not work!
    }

    /**
     * Returns the contents of a particular file (in text/xml format)
     * @param fileName name of the requested file
     * @return contents of the requested file
     * @throws SiteMapNotFoundException when the requested file is not found
     */
    @GetMapping(value = {"file", "file.xml"}, produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<InputStreamResource> fileXml(@RequestParam(value = "name", defaultValue = "")
                           @Pattern(regexp = FILENAME_REGEX, message = INVALID_FILENAME_MSG) String fileName) throws SiteMapNotFoundException {
        LOG.debug("Retrieving xml file {} ", fileName);
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Please provide a file name");
        }
        InputStreamResource result = new InputStreamResource(service.getFileAsStream(fileName));
        return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(result); // if we don't set content-type gzip will not work!

    }
}
