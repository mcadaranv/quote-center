package com.materials.quoting.client.metals;

/**
 * Thrown when the metals pricing API is unreachable or returns an error
 * after all retry attempts have been exhausted.
 */
public class MetalsPricingException extends RuntimeException {

    public MetalsPricingException(String message) {
        super(message);
    }

    public MetalsPricingException(String message, Throwable cause) {
        super(message, cause);
    }
}

