package com.ecommerce.monolith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login verifies a BCrypt hash, which is slow on purpose: it must not run inside a
 * transaction, and the user lookups around it must be read-only.
 */
// Scheduling off: a job running during the login would open its own write transaction
@SpringBootTest(properties = {"logging.level.org.springframework.orm.jpa=DEBUG", "app.scheduling.enabled=false"})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class TransactionBoundaryTests {

    @Autowired MockMvc mvc;

    @Test
    void loginOpensOnlyReadOnlyTransactions(CapturedOutput output) throws Exception {
        int from = output.getOut().length();
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin-password\"}"))
                .andExpect(status().isOk());

        List<String> created = Arrays.stream(output.getOut().substring(from).split("\\R"))
                .filter(line -> line.contains("Creating new transaction with name"))
                .toList();
        assertThat(created).isNotEmpty();
        assertThat(created).noneMatch(line -> line.contains("AuthService.login"));
        assertThat(created).allMatch(line -> line.contains("readOnly"));
    }
}
