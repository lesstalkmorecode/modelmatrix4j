package com.modelmatrix4j.tool;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ToolArgumentValidatorTest {
    private final ToolArgumentValidator validator = new ToolArgumentValidator();

    @Test
    void acceptsAnySyntacticallyValidJsonValue() {
        assertTrue(validator.isValid("{\"city\":\"Amsterdam\"}"));
        assertTrue(validator.isValid("[1,2,3]"));
        assertTrue(validator.isValid("42"));
    }

    @Test
    void rejectsMalformedDuplicateKeyAndTrailingTokenJson() {
        assertFalse(validator.isValid("{broken"));
        assertFalse(validator.isValid("{\"city\":1,\"city\":2}"));
        assertFalse(validator.isValid("{} {}"));
    }
}
