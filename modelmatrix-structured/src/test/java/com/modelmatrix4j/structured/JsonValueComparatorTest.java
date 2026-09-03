package com.modelmatrix4j.structured;

import static com.modelmatrix4j.structured.JsonValueComparator.Outcome.BOTH_INVALID;
import static com.modelmatrix4j.structured.JsonValueComparator.Outcome.DIFFERENT;
import static com.modelmatrix4j.structured.JsonValueComparator.Outcome.EQUIVALENT;
import static com.modelmatrix4j.structured.JsonValueComparator.Outcome.INVALID_LEFT;
import static com.modelmatrix4j.structured.JsonValueComparator.Outcome.INVALID_RIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JsonValueComparatorTest {

    private final JsonValueComparator comparator = new JsonValueComparator();

    @Test
    void ignoresObjectMemberOrderAndWhitespace() {
        assertEquals(
                EQUIVALENT,
                comparator.compare(
                        "{\"name\":\"Ada\",\"age\":37}",
                        " { \"age\" : 37, \"name\" : \"Ada\" } "
                )
        );
    }

    @Test
    void comparesNumbersByNumericValue() {
        assertEquals(
                EQUIVALENT,
                comparator.compare(
                        "{\"value\":1}",
                        "{\"value\":1.0}"
                )
        );
    }

    @Test
    void preservesArrayOrder() {
        assertEquals(
                DIFFERENT,
                comparator.compare("[1,2,3]", "[3,2,1]")
        );
    }

    @Test
    void distinguishesMissingFromNull() {
        assertEquals(
                DIFFERENT,
                comparator.compare("{}", "{\"value\":null}")
        );
    }

    @Test
    void reportsDuplicateObjectMembersAsInvalid() {
        assertEquals(
                INVALID_LEFT,
                comparator.compare(
                        "{\"value\":1,\"value\":1}",
                        "{\"value\":1}"
                )
        );
    }

    @Test
    void reportsMalformedInputsSeparatelyFromValueDifferences() {
        assertEquals(BOTH_INVALID, comparator.compare("{broken", "{broken"));
        assertEquals(INVALID_LEFT, comparator.compare("{broken", "{}"));
        assertEquals(INVALID_RIGHT, comparator.compare("{}", "{broken"));
    }

    @Test
    void syntacticComparisonDoesNotPretendToValidateSchemas() {
        assertEquals(
                EQUIVALENT,
                comparator.compare(
                        "{\"age\":\"abc\"}",
                        "{\"age\":\"abc\"}"
                )
        );
    }

    @Test
    void programmingContractViolationsAreNotConvertedToInvalidJson() {
        assertThrows(NullPointerException.class, () -> comparator.compare(null, "{}"));
        assertThrows(NullPointerException.class, () -> comparator.compare("{}", null));
    }
}
