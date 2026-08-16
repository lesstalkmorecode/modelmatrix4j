package com.modelmatrix4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CoreModuleSmokeTest {

    @Test
    void coreTestLifecycleRunsOnJava25() {
        assertEquals(25, Runtime.version().feature());
    }
}
