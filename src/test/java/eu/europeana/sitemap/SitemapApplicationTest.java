package eu.europeana.sitemap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource("classpath:sitemap-test.properties")
public class SitemapApplicationTest {

	/**
	 * Basic Spring-Boot context load test
	 */
	@Test
	public void contextLoads() {
		// empty method, this simply tests application loading
	}

}
