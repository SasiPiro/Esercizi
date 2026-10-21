package com.ecommerce.monolith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class RequestIdTests {

    @Autowired MockMvc mvc;

    @Test
    void everyResponseCarriesAGeneratedRequestId() throws Exception {
        String id = mvc.perform(get("/api/v1/cart"))
                .andExpect(header().exists("X-Request-Id"))
                .andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(id).matches("[0-9a-f-]{36}");
    }

    @Test
    void incomingIdIsPropagatedToTheResponseAndToTheLogs(CapturedOutput output) throws Exception {
        mvc.perform(post("/api/v1/auth/login").header("X-Request-Id", "checkout-test-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(header().string("X-Request-Id", "checkout-test-42"));

        assertThat(output.getOut()).containsPattern("\\[checkout-test-42] .*\\[AUTH_LOGIN] outcome=failure");
    }

    @Test
    void unsafeIncomingIdIsReplacedToPreventLogInjection() throws Exception {
        String id = mvc.perform(get("/api/v1/cart").header("X-Request-Id", "x INFO [AUTH_LOGIN] outcome=success"))
                .andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(id).matches("[0-9a-f-]{36}");
    }
}
