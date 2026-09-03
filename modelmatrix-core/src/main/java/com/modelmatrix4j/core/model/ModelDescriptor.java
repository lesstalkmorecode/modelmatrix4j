package com.modelmatrix4j.core.model;

import java.util.Objects;

/**
 * Stable identity of one declared model configuration within a matrix.
 *
 * @param configurationId non-blank identifier used in run identity, ordering, correlation, and reports
 */
public record ModelDescriptor(String configurationId) {
    /** @throws IllegalArgumentException if {@code configurationId} is blank */
    public ModelDescriptor {
        Objects.requireNonNull(configurationId, "configurationId");
        if (configurationId.isBlank()) {
            throw new IllegalArgumentException("configurationId must not be blank");
        }
    }
}
