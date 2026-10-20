package com.example.esercizi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Log lines are part of the contract with whoever operates the application:
 * one consistent [EVENT] key=value format, the right level, no duplicates, no secrets.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class LoggingTests {

    @Autowired MockMvc mvc;

    private Locale originalLocale;

    @BeforeEach
    void italianServer() {
        // Reproduce a server running with an Italian locale (decimal comma)
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.ITALY);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @Test
    void failedLoginIsOneWarnWithUsernameAndNoPassword(CapturedOutput output) throws Exception {
        int from = output.getOut().length();
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"S3cret-typed-by-user\"}"))
                .andExpect(status().isUnauthorized());

        String logs = output.getOut().substring(from);
        assertThat(logs).contains("WARN").contains("[AUTH_LOGIN] outcome=failure username=admin reason=BadCredentialsException");
        assertThat(logs).doesNotContain("S3cret-typed-by-user");
        assertThat(count(logs, "[AUTH_LOGIN]")).isEqualTo(1);
        assertThat(logs).doesNotContain("[HTTP_SERVER_ERROR]");
    }

    @Test
    void duplicateRegistrationIs409AndNotAServerError(CapturedOutput output) throws Exception {
        int from = output.getOut().length();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"JAY\",\"password\":\"secret123\"}"))
                .andExpect(status().isConflict());

        String logs = output.getOut().substring(from);
        assertThat(logs).contains("[AUTH_REGISTER] outcome=rejected username=JAY reason=duplicate_username");
        assertThat(logs).doesNotContain("[HTTP_SERVER_ERROR]").doesNotContain(" ERROR ");
    }

    @Test
    void businessRejectionIsLoggedOnceAndNotAsError(CapturedOutput output) throws Exception {
        String guest = guestToken();
        int from = output.getOut().length();
        mvc.perform(post("/api/v1/checkout").header("Authorization", "Bearer " + guest))
                .andExpect(status().isBadRequest()); // empty cart

        String logs = output.getOut().substring(from);
        assertThat(logs).contains("[PRICING] operation=placeOrder outcome=rejected")
                .contains("reason=CartEmptyException");
        assertThat(logs).contains("[HTTP_CLIENT_ERROR] status=400");
        assertThat(logs).doesNotContain(" ERROR ");
    }

    @Test
    void checkoutLineUsesADecimalPointWhateverTheServerLocale(CapturedOutput output) throws Exception {
        String user = loginNewUser();
        mvc.perform(post("/api/v1/cart/add").param("productCode", "P001").param("quantity", "3")
                .header("Authorization", "Bearer " + user)).andExpect(status().isOk());
        int from = output.getOut().length();
        mvc.perform(post("/api/v1/checkout").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk());

        String logs = output.getOut().substring(from);
        assertThat(logs).containsPattern(
                "\\[PRICING] operation=placeOrder outcome=success principal=\\S+ initial=31\\.50 final=31\\.50 "
                        + "saved=0\\.00 discountPct=0\\.00 strategies=\\[] coupon=null durationMs=\\d+");
    }

    private static int count(String text, String token) {
        return text.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }

    private String guestToken() throws Exception {
        return JsonPath.read(mvc.perform(post("/api/v1/auth/guest"))
                .andReturn().getResponse().getContentAsString(), "$.token");
    }

    private String loginNewUser() throws Exception {
        String credentials = "{\"username\":\"lg" + UUID.randomUUID().toString().substring(0, 8)
                + "\",\"password\":\"secret123\"}";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());
        return JsonPath.read(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials)).andReturn().getResponse().getContentAsString(), "$.token");
    }
}
