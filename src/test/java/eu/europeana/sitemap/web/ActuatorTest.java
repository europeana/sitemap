package eu.europeana.sitemap.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit test for testing if the /actuator/info endpoint is available
 */
@SpringBootTest
@TestPropertySource("classpath:sitemap-test.properties")
@AutoConfigureMockMvc
public class ActuatorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testActuatorInfo() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/info"))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();
        // also check that there are contents
        assert result.getResponse().getContentAsString().contains("app");
    }

}
