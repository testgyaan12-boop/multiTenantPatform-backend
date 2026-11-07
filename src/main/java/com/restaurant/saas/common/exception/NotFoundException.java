package com.restaurant.saas.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "resource_not_found", resource + " not found: " + id);
    }

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "resource_not_found", message);
    }
}
