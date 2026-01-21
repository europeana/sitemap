package eu.europeana.sitemap.web;

import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.service.ReadSitemapService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

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
     * Let request to homepage (root) redirect to portal
     * @return redirect to www.europeana.eu
     */
    @GetMapping
    public void homepage(HttpServletResponse httpServletResponse) {
            httpServletResponse.setHeader("Location", "https://www.europeana.eu");
            httpServletResponse.setStatus(HttpServletResponse.SC_FOUND);
    }

    /**
     * Lists all files stored in the used bucket (for debugging purposes)
     * @return list of all files in the bucket
     */
    @GetMapping(value = {"list", "files"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> filesStream() {
        StreamingResponseBody responseBody = service::getFilesAsStream;
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(responseBody);
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

    // TMP for testing fluentd issues
    @GetMapping(value = "error1")
    public void generateNormalMessage() {
        LOG.error("This is a normal error message with UTF-8 characters.");
    }

    @GetMapping(value = "error2")
    public void generateNonUtf8Message() throws UnsupportedEncodingException {
        ThreadContext.put("CHARSET", "UTF-16");
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < 254; i++) {
                sb.append(new String(new byte[]{(byte)i}, StandardCharsets.UTF_8));
        }
        LOG.error("This is an error message with non-UTF-8 characters in it: {} END OF STRING", sb);
    }

    @GetMapping(value = "error3")
    public void generateNonUtf8Message2() {
        ThreadContext.put("CHARSET", "UTF-16");
        char char1 = '\u0080';
        char char2 = '\u0083';
        char char3 = '\u0099';
        LOG.error("Second attempt to generate non-utf characters: " + char1 + char2 + char3 + " END OF STRING");
    }

    @GetMapping(value = "stack1")
    public void generateNormalStacktrace() {
        LOG.error("Normal stacktrace consisting of around 7687 characters: \n" +
                "Error response (400): An error occurred during user set validation! Invalid property value. page : value out of range: 7, last page:1\n" +
                "eu.europeana.api.commons.web.exception.ParamValidationException: error.userset_validation_property_value\n" +
                "\tat eu.europeana.set.web.service.impl.BaseUserSetServiceImpl.validateLastPage(BaseUserSetServiceImpl.java:999) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.impl.UserSetServiceImpl.fetchUserSetItems(UserSetServiceImpl.java:569) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.getUserSetPage(WebUserSetRest.java:256) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.processRetrieveSetPageRequest(WebUserSetRest.java:197) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.getUserSet(WebUserSetRest.java:168) ~[classes!/:?]\n" +
                "\tat jdk.internal.reflect.GeneratedMethodAccessor181.invoke(Unknown Source) ~[?:?]\n" +
                "\tat jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source) ~[?:?]\n" +
                "\tat java.lang.reflect.Method.invoke(Unknown Source) ~[?:?]\n" +
                "\tat org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:205) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:150) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:117) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:895) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:808) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1067) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:963) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:655) ~[tomcat-embed-core-9.0.63.jar!/:2.5]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:764) ~[tomcat-embed-core-9.0.63.jar!/:2.5]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:227) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.doFilterInternal(WebMvcMetricsFilter.java:96) ~[spring-boot-actuator-2.5.14.jar!/:2.5.14]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:197) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:541) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:135) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:360) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:399) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:890) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1743) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat java.lang.Thread.run(Unknown Source) ~[?:?]");
    }

    @GetMapping(value = "stack2")
    public void generateVeryLongMessage() {
        LOG.error("Very long stacktrace: \n" +
                "Error response (400): An error occurred during user set validation! Invalid property value. page : value out of range: 7, last page:1\n" +
                "eu.europeana.api.commons.web.exception.ParamValidationException: error.userset_validation_property_value\n" +
                "\tat eu.europeana.set.web.service.impl.BaseUserSetServiceImpl.validateLastPage(BaseUserSetServiceImpl.java:999) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.impl.UserSetServiceImpl.fetchUserSetItems(UserSetServiceImpl.java:569) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.getUserSetPage(WebUserSetRest.java:256) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.processRetrieveSetPageRequest(WebUserSetRest.java:197) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.getUserSet(WebUserSetRest.java:168) ~[classes!/:?]\n" +
                "\tat jdk.internal.reflect.GeneratedMethodAccessor181.invoke(Unknown Source) ~[?:?]\n" +
                "\tat jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source) ~[?:?]\n" +
                "\tat java.lang.reflect.Method.invoke(Unknown Source) ~[?:?]\n" +
                "\tat org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:205) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:150) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:117) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:895) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:808) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1067) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:963) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:655) ~[tomcat-embed-core-9.0.63.jar!/:2.5]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:764) ~[tomcat-embed-core-9.0.63.jar!/:2.5]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:227) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.doFilterInternal(WebMvcMetricsFilter.java:96) ~[spring-boot-actuator-2.5.14.jar!/:2.5.14]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:197) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:541) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:135) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:360) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:399) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:890) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1743) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat java.lang.Thread.run(Unknown Source) ~[?:?]\n" +
                "\tat eu.europeana.set.web.service.impl.BaseUserSetServiceImpl.validateLastPage(BaseUserSetServiceImpl.java:999) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.impl.UserSetServiceImpl.fetchUserSetItems(UserSetServiceImpl.java:569) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.getUserSetPage(WebUserSetRest.java:256) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.processRetrieveSetPageRequest(WebUserSetRest.java:197) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.getUserSet(WebUserSetRest.java:168) ~[classes!/:?]\n" +
                "\tat jdk.internal.reflect.GeneratedMethodAccessor181.invoke(Unknown Source) ~[?:?]\n" +
                "\tat jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source) ~[?:?]\n" +
                "\tat java.lang.reflect.Method.invoke(Unknown Source) ~[?:?]\n" +
                "\tat org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:205) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:150) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:117) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:895) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:808) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1067) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:963) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:655) ~[tomcat-embed-core-9.0.63.jar!/:2.5]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:764) ~[tomcat-embed-core-9.0.63.jar!/:2.5]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:227) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.doFilterInternal(WebMvcMetricsFilter.java:96) ~[spring-boot-actuator-2.5.14.jar!/:2.5.14]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:197) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:541) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:135) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:360) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:399) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:890) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1743) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat java.lang.Thread.run(Unknown Source) ~[?:?]" +
                "\tat eu.europeana.set.web.service.impl.BaseUserSetServiceImpl.validateLastPage(BaseUserSetServiceImpl.java:999) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.impl.UserSetServiceImpl.fetchUserSetItems(UserSetServiceImpl.java:569) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.getUserSetPage(WebUserSetRest.java:256) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.processRetrieveSetPageRequest(WebUserSetRest.java:197) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.getUserSet(WebUserSetRest.java:168) ~[classes!/:?]\n" +
                "\tat jdk.internal.reflect.GeneratedMethodAccessor181.invoke(Unknown Source) ~[?:?]\n" +
                "\tat jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source) ~[?:?]\n" +
                "\tat java.lang.reflect.Method.invoke(Unknown Source) ~[?:?]\n" +
                "\tat org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:205) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:150) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:117) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:895) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:808) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1067) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:963) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:655) ~[tomcat-embed-core-9.0.63.jar!/:2.5]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:764) ~[tomcat-embed-core-9.0.63.jar!/:2.5]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:227) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.doFilterInternal(WebMvcMetricsFilter.java:96) ~[spring-boot-actuator-2.5.14.jar!/:2.5.14]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:197) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:541) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:135) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:360) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:399) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:890) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1743) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat java.lang.Thread.run(Unknown Source) ~[?:?]" +
                "\tat eu.europeana.set.web.service.impl.BaseUserSetServiceImpl.validateLastPage(BaseUserSetServiceImpl.java:999) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.impl.UserSetServiceImpl.fetchUserSetItems(UserSetServiceImpl.java:569) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.getUserSetPage(WebUserSetRest.java:256) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.processRetrieveSetPageRequest(WebUserSetRest.java:197) ~[classes!/:?]\n" +
                "\tat eu.europeana.set.web.service.controller.jsonld.WebUserSetRest.getUserSet(WebUserSetRest.java:168) ~[classes!/:?]\n" +
                "\tat jdk.internal.reflect.GeneratedMethodAccessor181.invoke(Unknown Source) ~[?:?]\n" +
                "\tat jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source) ~[?:?]\n" +
                "\tat java.lang.reflect.Method.invoke(Unknown Source) ~[?:?]\n" +
                "\tat org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:205) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:150) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:117) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:895) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:808) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1067) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:963) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:655) ~[tomcat-embed-core-9.0.63.jar!/:2.5]\n" +
                "\tat org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) ~[spring-webmvc-5.3.20.jar!/:5.3.20]\n" +
                "\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:764) ~[tomcat-embed-core-9.0.63.jar!/:2.5]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:227) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.doFilterInternal(WebMvcMetricsFilter.java:96) ~[spring-boot-actuator-2.5.14.jar!/:2.5.14]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.20.jar!/:5.3.20]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:197) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:541) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:135) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:360) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:399) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:890) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1743) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-9.0.63.jar!/:?]\n" +
                "\tat java.lang.Thread.run(Unknown Source) ~[?:?]");
    }
}
