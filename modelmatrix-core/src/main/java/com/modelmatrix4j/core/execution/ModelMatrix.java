package com.modelmatrix4j.core.execution;

import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.CompatibilityStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * High-level facade for executing one scenario against an ordered matrix of model configurations.
 *
 * <p>The matrix owns execution lifecycle concerns such as repetitions, timeout, concurrency
 * admission, deterministic result ordering, and terminal run classification. Each physical model
 * invocation is delegated exactly once to the configured {@link ModelUnderTest} adapter.</p>
 */
public final class ModelMatrix {
    private final List<ModelUnderTest> models;
    private final ExecutionSettings executionSettings;

    private ModelMatrix(List<ModelUnderTest> models, ExecutionSettings executionSettings) {
        this.models = models;
        this.executionSettings = executionSettings;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @return immutable compatibility result in deterministic model/repetition order */
    public CompatibilityResult run(Scenario scenario) {
        Objects.requireNonNull(scenario, "scenario");
        MatrixExecutor executor = new MatrixExecutor();
        List<ExecutionOutcome> outcomes = executor.execute(scenario, models, executionSettings);
        CompatibilityEvaluator evaluator = new CompatibilityEvaluator();
        CompatibilityStatus status = evaluator.evaluate(outcomes);
        RunResultMapper mapper = new RunResultMapper();
        return new CompatibilityResult(status, mapper.map(outcomes));
    }

    public static final class Builder {
        private List<ModelUnderTest> models;
        private int repetitions;
        private Duration timeout;
        private int maxConcurrentInvocations;

        private Builder() {
            ExecutionSettings defaults = ExecutionSettings.defaults();
            this.repetitions = defaults.repetitions();
            this.timeout = defaults.timeout();
            this.maxConcurrentInvocations = defaults.maxConcurrentInvocations();
        }

        public Builder models(ModelUnderTest... models) {
            return models(List.of(Objects.requireNonNull(models, "models")));
        }

        public Builder models(List<ModelUnderTest> models) {
            this.models = List.copyOf(Objects.requireNonNull(models, "models"));
            return this;
        }

        /** Sets how many times each model configuration is executed. */
        public Builder repetitions(int repetitions) {
            this.repetitions = repetitions;
            return this;
        }

        /** Sets the timeout budget for one repetition, including concurrency-admission wait. */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /** Sets the maximum number of physically active/admitted model invocations. */
        public Builder maxConcurrentInvocations(int maxConcurrentInvocations) {
            this.maxConcurrentInvocations = maxConcurrentInvocations;
            return this;
        }

        /**
         * @throws IllegalStateException if models were never configured
         * @throws IllegalArgumentException if fewer than two model configurations are supplied,
         *         configuration identifiers are duplicated, or execution settings are invalid
         */
        public ModelMatrix build() {
            if (models == null) {
                throw new IllegalStateException("models must be configured");
            }
            List<ModelUnderTest> targets = Objects.requireNonNull(models, "models");
            if (targets.size() < 2) {
                throw new IllegalArgumentException(
                        "at least two model configurations are required for compatibility");
            }
            HashSet<String> configurationIds = new HashSet<>();
            for (ModelUnderTest target : targets) {
                if (!configurationIds.add(target.descriptor().configurationId())) {
                    throw new IllegalArgumentException(
                            "duplicate configurationId: " + target.descriptor().configurationId());
                }
            }
            return new ModelMatrix(targets,
                    new ExecutionSettings(repetitions, timeout, maxConcurrentInvocations));
        }
    }
}
