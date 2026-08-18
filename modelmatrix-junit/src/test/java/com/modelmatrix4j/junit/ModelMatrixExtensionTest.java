package com.modelmatrix4j.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.model.ModelUnavailableException;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.CompatibilityStatus;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

class ModelMatrixExtensionTest {

    @Test
    void discoversAndInjectsOneOrderedRunPerModel() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(CompatibleFixture.class))
                .execute()
                .testEvents()
                .assertStatistics(statistics -> statistics.started(1).succeeded(1).failed(0));
    }

    @Test
    void rejectsInvalidSourceBeforeTestInvocation() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(InvalidFixture.class))
                .execute()
                .testEvents()
                .assertStatistics(statistics -> statistics.started(1).succeeded(0).failed(1));
    }

    @Test
    void keepsMismatchUnavailableAndFailureDistinct() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(MismatchFixture.class), selectClass(UnavailableFixture.class),
                        selectClass(FailureFixture.class))
                .execute()
                .testEvents()
                .assertStatistics(statistics -> statistics.started(3).succeeded(3).failed(0));
    }

    @Test
    void parallelInvocationsRemainIsolated() {
        EngineTestKit.engine("junit-jupiter")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent")
                .configurationParameter("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
                .selectors(selectClass(ParallelAlphaFixture.class), selectClass(ParallelBetaFixture.class))
                .execute()
                .testEvents()
                .assertStatistics(statistics -> statistics.started(2).succeeded(2).failed(0));
    }

    static class CompatibleFixture implements ModelMatrixSource {
        @Override
        public Scenario scenario() {
            return new Scenario("greeting", "hello");
        }

        @Override
        public List<ModelUnderTest> models() {
            return List.of(model("first", " same  output "), model("second", "same output"));
        }

        @ModelMatrixTest
        void compares(CompatibilityResult result) {
            assertEquals(CompatibilityStatus.COMPATIBLE, result.status());
            assertEquals(List.of("first", "second"), result.runs().stream()
                    .map(run -> run.model().configurationId()).toList());
            assertEquals(List.of(0, 0), result.runs().stream().map(run -> run.repetition()).toList());
            assertEquals(List.of(RunStatus.COMPLETED, RunStatus.COMPLETED),
                    result.runs().stream().map(run -> run.status()).toList());
        }
    }

    static class InvalidFixture implements ModelMatrixSource {
        @Override
        public Scenario scenario() {
            return new Scenario("invalid", "input");
        }

        @Override
        public List<ModelUnderTest> models() {
            return List.of();
        }

        @ModelMatrixTest
        void mustNotRun(CompatibilityResult result) {
            throw new AssertionError("invalid configuration reached the test method");
        }
    }

    static class MismatchFixture implements ModelMatrixSource {
        @Override
        public Scenario scenario() {
            return new Scenario("mismatch", "input");
        }

        @Override
        public List<ModelUnderTest> models() {
            return List.of(model("a", "one"), model("b", "two"));
        }

        @ModelMatrixTest
        void mismatch(CompatibilityResult result) {
            assertEquals(CompatibilityStatus.MISMATCH, result.status());
        }
    }

    static class UnavailableFixture implements ModelMatrixSource {
        @Override
        public Scenario scenario() {
            return new Scenario("unavailable", "input");
        }

        @Override
        public List<ModelUnderTest> models() {
            return List.of(model("a", "one"), ModelMatrixExtensionTest.unavailable("b"));
        }

        @ModelMatrixTest
        void unavailable(CompatibilityResult result) {
            assertEquals(CompatibilityStatus.UNAVAILABLE, result.status());
        }
    }

    static class FailureFixture implements ModelMatrixSource {
        @Override
        public Scenario scenario() {
            return new Scenario("failure", "input");
        }

        @Override
        public List<ModelUnderTest> models() {
            return List.of(model("a", "one"), failing("b"));
        }

        @ModelMatrixTest
        void failure(CompatibilityResult result) {
            assertEquals(CompatibilityStatus.EXECUTION_FAILURE, result.status());
            assertNotEquals(CompatibilityStatus.MISMATCH, result.status());
        }
    }

    abstract static class ParallelFixture implements ModelMatrixSource {
        private static final CyclicBarrier INVOCATIONS_READY = new CyclicBarrier(2);

        abstract String expected();

        @Override
        public Scenario scenario() {
            return new Scenario(expected(), expected());
        }

        @Override
        public List<ModelUnderTest> models() {
            return List.of(new ModelUnderTest(new ModelDescriptor(expected() + "-model"), ignored -> {
                INVOCATIONS_READY.await(2, TimeUnit.SECONDS);
                return expected();
            }));
        }
    }

    static class ParallelAlphaFixture extends ParallelFixture {
        @Override
        String expected() {
            return "alpha";
        }

        @ModelMatrixTest
        void alpha(CompatibilityResult result) {
            assertOwnResult(result, "alpha");
        }
    }

    static class ParallelBetaFixture extends ParallelFixture {
        @Override
        String expected() {
            return "beta";
        }

        @ModelMatrixTest
        void beta(CompatibilityResult result) {
            assertOwnResult(result, "beta");
        }
    }

    private static void assertOwnResult(CompatibilityResult result, String expected) {
        assertEquals(CompatibilityStatus.COMPATIBLE, result.status());
        assertEquals(expected, result.runs().getFirst().scenarioId());
        assertEquals(expected, result.runs().getFirst().output());
    }

    private static ModelUnderTest model(String id, String output) {
        return new ModelUnderTest(new ModelDescriptor(id), ignored -> output);
    }

    private static ModelUnderTest unavailable(String id) {
        return new ModelUnderTest(new ModelDescriptor(id), ignored -> {
            throw new ModelUnavailableException("offline");
        });
    }

    private static ModelUnderTest failing(String id) {
        return new ModelUnderTest(new ModelDescriptor(id), ignored -> {
            throw new IllegalStateException("failed");
        });
    }
}
