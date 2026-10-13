package com.pha.common.error;

/**
 * Thrown when a requested resource does not exist. The message is returned to clients, so it must not contain PII.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, Object id) {
        super(resourceType + " " + id + " was not found");
    }
}
