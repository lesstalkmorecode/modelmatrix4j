package com.modelmatrix4j.core.model;

/** Signals that a configured model/provider cannot service the invocation. */
public final class ModelUnavailableException extends Exception {
    /**
     * Creates an unavailability failure with a diagnostic message.
     *
     * @param message diagnostic describing the unavailable model/provider
     */
    public ModelUnavailableException(String message) {
        super(message);
    }
}
