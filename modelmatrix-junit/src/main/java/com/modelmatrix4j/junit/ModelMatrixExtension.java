package com.modelmatrix4j.junit;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public final class ModelMatrixExtension implements BeforeEachCallback, ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(ModelMatrixExtension.class);
    private static final String RESULT_KEY = "compatibility-result";

    @Override
    public void beforeEach(ExtensionContext context) {
        Object testInstance = context.getRequiredTestInstance();
        if (!(testInstance instanceof ModelMatrixSource source)) {
            throw new ExtensionConfigurationException(
                    "@ModelMatrixTest requires the test instance to implement ModelMatrixSource");
        }

        try {
            Scenario scenario = source.scenario();
            List<ModelUnderTest> models = source.models();
            int repetitions = source.repetitions();
            Duration timeout = source.timeout();
            int maxConcurrentInvocations = source.maxConcurrentInvocations();
            CompatibilityResult result = ModelMatrix.builder()
                    .models(models)
                    .repetitions(repetitions)
                    .timeout(timeout)
                    .maxConcurrentInvocations(maxConcurrentInvocations)
                    .build()
                    .run(scenario);
            context.getStore(NAMESPACE).put(RESULT_KEY, result);
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw new ExtensionConfigurationException("Invalid ModelMatrixSource configuration", exception);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == CompatibilityResult.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        CompatibilityResult result = context.getStore(NAMESPACE).get(RESULT_KEY, CompatibilityResult.class);
        if (result == null) {
            throw new ParameterResolutionException("CompatibilityResult is unavailable for this invocation");
        }
        return result;
    }
}
