package com.modelmatrix4j.structured;

import java.util.Comparator;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Compares JSON values by data semantics rather than serialization details. */
public final class JsonValueComparator {

    /** Result of parsing and comparing two JSON values. */
    public enum Outcome {
        /** Both inputs are valid and semantically equivalent. */
        EQUIVALENT,
        /** Both inputs are valid but semantically different. */
        DIFFERENT,
        /** Only the left input is invalid JSON. */
        INVALID_LEFT,
        /** Only the right input is invalid JSON. */
        INVALID_RIGHT,
        /** Both inputs are invalid JSON. */
        BOTH_INVALID
    }

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private static final Comparator<JsonNode> JSON_VALUE_COMPARATOR = (left, right) -> {
        if (left.isNumber() && right.isNumber()) {
            return left.decimalValue().compareTo(right.decimalValue());
        }
        return left.equals(right) ? 0 : 1;
    };

    /**
     * Tests whether the input contains exactly one valid JSON value.
     *
     * @param json JSON text
     * @return whether the input satisfies the canonical parser rules
     * @throws NullPointerException if {@code json} is {@code null}
     */
    public boolean isValid(String json) {
        Objects.requireNonNull(json, "json");
        return parse(json).valid();
    }

    /**
     * Compares two JSON values semantically.
     *
     * <p>Object member order is irrelevant, array order is significant, numeric representations
     * such as {@code 1} and {@code 1.0} compare equal, and malformed, duplicate-key, or
     * trailing-token inputs are reported explicitly.</p>
     *
     * @param left left JSON value
     * @param right right JSON value
     * @return semantic comparison outcome
     * @throws NullPointerException if either input is {@code null}
     */
    public Outcome compare(String left, String right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");

        ParsedJson leftValue = parse(left);
        ParsedJson rightValue = parse(right);

        if (!leftValue.valid() && !rightValue.valid()) {
            return Outcome.BOTH_INVALID;
        }
        if (!leftValue.valid()) {
            return Outcome.INVALID_LEFT;
        }
        if (!rightValue.valid()) {
            return Outcome.INVALID_RIGHT;
        }

        return leftValue.value().equals(JSON_VALUE_COMPARATOR, rightValue.value())
                ? Outcome.EQUIVALENT
                : Outcome.DIFFERENT;
    }

    private static ParsedJson parse(String json) {
        try {
            JsonNode value = MAPPER.readTree(json);
            return value == null
                    ? ParsedJson.invalid()
                    : ParsedJson.valid(value);
        } catch (JacksonException exception) {
            return ParsedJson.invalid();
        }
    }

    private record ParsedJson(JsonNode value, boolean valid) {
        private static ParsedJson valid(JsonNode value) {
            return new ParsedJson(value, true);
        }

        private static ParsedJson invalid() {
            return new ParsedJson(null, false);
        }
    }
}
