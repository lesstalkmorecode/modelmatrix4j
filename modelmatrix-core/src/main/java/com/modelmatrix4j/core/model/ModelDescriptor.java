package com.modelmatrix4j.core.model;

import java.util.Objects;

public record ModelDescriptor(String configurationId) {
    public ModelDescriptor {
        Objects.requireNonNull(configurationId, "configurationId");
        if (configurationId.isBlank()) {
            throw new IllegalArgumentException("configurationId must not be blank");
        }
    }
}
