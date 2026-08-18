package com.modelmatrix4j.core.execution;

import com.modelmatrix4j.core.result.CompatibilityStatus;
import com.modelmatrix4j.core.result.RunStatus;
import java.util.List;

/** Evaluates matrix compatibility from pre-redaction normalized outcomes. */
final class CompatibilityEvaluator {
    CompatibilityStatus evaluate(List<ExecutionOutcome> outcomes) {
        boolean unavailable = false;
        boolean mismatch = false;
        String expected = null;
        for (ExecutionOutcome outcome : outcomes) {
            switch (outcome.state()) {
                case ExecutionOutcome.Failed failure -> {
                    if (failure.status() != RunStatus.UNAVAILABLE) {
                        return CompatibilityStatus.EXECUTION_FAILURE;
                    }
                    unavailable = true;
                }
                case ExecutionOutcome.Completed success -> {
                    if (expected == null) {
                        expected = success.normalizedOutput();
                    } else if (!expected.equals(success.normalizedOutput())) {
                        mismatch = true;
                    }
                }
            }
        }
        if (unavailable) {
            return CompatibilityStatus.UNAVAILABLE;
        }
        return mismatch ? CompatibilityStatus.MISMATCH : CompatibilityStatus.COMPATIBLE;
    }
}

