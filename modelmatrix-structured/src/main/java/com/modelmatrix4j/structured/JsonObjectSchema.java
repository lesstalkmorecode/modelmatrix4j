package com.modelmatrix4j.structured;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Validates required top-level fields and value types for a JSON object. */
public final class JsonObjectSchema {

    public enum ValueType {
        OBJECT,
        ARRAY,
        STRING,
        NUMBER,
        BOOLEAN,
        NULL
    }

    /** Diagnostic is empty exactly when validation succeeds. */
    public record Validation(boolean valid, String diagnostic) {
        public Validation {
            Objects.requireNonNull(diagnostic, "diagnostic");
            if (valid && !diagnostic.isEmpty()) {
                throw new IllegalArgumentException("valid result must not contain a diagnostic");
            }
            if (!valid && diagnostic.isBlank()) {
                throw new IllegalArgumentException("invalid result must contain a diagnostic");
            }
        }
    }

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private final Map<String, ValueType> requiredFields;

    /** Required-field iteration order is preserved; field names must be non-blank. */
    public JsonObjectSchema(Map<String, ValueType> requiredFields) {
        Objects.requireNonNull(requiredFields, "requiredFields");
        LinkedHashMap<String, ValueType> copy = new LinkedHashMap<>();
        requiredFields.forEach((name, type) -> {
            Objects.requireNonNull(name, "field name");
            Objects.requireNonNull(type, "field type");
            if (name.isBlank()) {
                throw new IllegalArgumentException("field name must not be blank");
            }
            copy.put(name, type);
        });
        this.requiredFields = Collections.unmodifiableMap(copy);
    }

    /**
     * Validates JSON syntax, top-level object shape, and required field value kinds. Duplicate keys
     * and trailing tokens are invalid; parser failures are returned as bounded diagnostics.
     */
    public Validation validate(String json) {
        Objects.requireNonNull(json, "json");

        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JacksonException exception) {
            return invalid("malformed JSON");
        }

        if (root == null) {
            return invalid("malformed JSON");
        }
        if (!root.isObject()) {
            return invalid("expected top-level JSON object");
        }

        for (Map.Entry<String, ValueType> entry : requiredFields.entrySet()) {
            JsonNode value = root.get(entry.getKey());
            if (value == null) {
                return invalid("missing required field: " + entry.getKey());
            }
            if (!matches(value, entry.getValue())) {
                return invalid(
                        "field " + entry.getKey()
                                + " must be " + entry.getValue().name().toLowerCase()
                );
            }
        }

        return new Validation(true, "");
    }

    /** Iteration order matches the order captured at construction. */
    public Map<String, ValueType> requiredFields() {
        return requiredFields;
    }

    private static boolean matches(JsonNode value, ValueType type) {
        return switch (type) {
            case OBJECT -> value.isObject();
            case ARRAY -> value.isArray();
            case STRING -> value.isString();
            case NUMBER -> value.isNumber();
            case BOOLEAN -> value.isBoolean();
            case NULL -> value.isNull();
        };
    }

    private static Validation invalid(String diagnostic) {
        return new Validation(false, diagnostic);
    }
}
