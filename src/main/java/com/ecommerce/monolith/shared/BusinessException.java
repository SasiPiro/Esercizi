package com.ecommerce.monolith.shared;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * A request rejected by a business rule (4xx), with the problem details it is reported
 * with: status, title and type.
 *
 * <p>
 * Each module's exceptions extend this and declare their own mapping, so the shared
 * exception handler turns any of them into a problem detail without knowing the modules
 * (it would otherwise depend on every one of them, and they all depend on shared).
 * </p>
 */
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String title;
    private final URI type;

    protected BusinessException(HttpStatus status, String title, URI type, String message) {
        super(message);
        this.status = status;
        this.title = title;
        this.type = type;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public URI getType() {
        return type;
    }
}
