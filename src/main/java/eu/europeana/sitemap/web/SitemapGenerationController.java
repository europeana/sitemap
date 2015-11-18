package eu.europeana.sitemap.web;


import eu.europeana.sitemap.exceptions.SitemapNotReadyException;
import eu.europeana.sitemap.service.SitemapService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


/**
 * Created by ymamakis on 11/16/15.
 */
@RestController
@RequestMapping("/")
public class SitemapGenerationController {

    @Resource
    private SitemapService service;

    @RequestMapping(value = "generate", method = RequestMethod.GET)
    public @ResponseBody String generate() throws SitemapNotReadyException{
        service.generate();
        return "OK";
    }

    @RequestMapping(value = "delete", method = RequestMethod.GET)
    public @ResponseBody String delete() throws SitemapNotReadyException{
        service.delete();
        return "OK";
    }
}
