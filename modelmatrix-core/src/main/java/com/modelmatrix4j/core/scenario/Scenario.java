package com.modelmatrix4j.core.scenario;

import java.util.Objects;

public record Scenario(String id, String input) {
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
