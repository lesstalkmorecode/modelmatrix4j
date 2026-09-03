package com.modelmatrix4j.core.scenario;

import java.util.Objects;

/**
 * Provider-neutral input executed unchanged across the declared model matrix.
 *
 * @param id non-blank stable scenario identifier used in run identity and reporting
 * @param input application/model input; may be empty but not {@code null}
 */
public record Scenario(String id, String input) {
    /** @throws IllegalArgumentException if {@code id} is blank */
    public Scenario {
        id = requireText(id, "id");
        input = Objects.requireNonNull(input, "input");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
