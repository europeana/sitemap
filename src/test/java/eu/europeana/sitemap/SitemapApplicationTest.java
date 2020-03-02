package eu.europeana.sitemap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("classpath:sitemap-test.properties")
public class SitemapApplicationTest {

	/**
	 * Basic Spring-Boot context load test
	 */
	@Test
	public void contextLoads() {
	}

}
