package com.example.esercizi.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every HTTP request an id and puts it in every log line written while serving it
 * (see logging.pattern.correlation), so all the lines of one request can be found together.
 *
 * <p>
 * The id is taken from the incoming {@code X-Request-Id} header when present (e.g. set by
 * an ingress, an API gateway or the frontend) and generated otherwise. It is returned in
 * the response header, so a client can quote it when reporting a problem. Incoming values
 * are accepted only if short and alphanumeric: a raw header in the logs would allow log
 * injection (fake lines through newlines).
 * </p>
 *
 * <p>Runs first, before Spring Security, so even authentication logs carry the id.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = incoming != null && SAFE_ID.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY); // threads are reused: never leak the id into the next request
        }
    }
}
