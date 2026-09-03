package com.modelmatrix4j.junit;

import com.modelmatrix4j.core.result.CompatibilityResult;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * JUnit Jupiter test annotation that executes the matrix supplied by {@link ModelMatrixSource}.
 *
 * <p>The containing test instance must implement {@link ModelMatrixSource}. A test method may
 * declare a {@link CompatibilityResult} parameter, which the extension resolves to the completed
 * core result for that invocation.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Test
@ExtendWith(ModelMatrixExtension.class)
public @interface ModelMatrixTest {
}
