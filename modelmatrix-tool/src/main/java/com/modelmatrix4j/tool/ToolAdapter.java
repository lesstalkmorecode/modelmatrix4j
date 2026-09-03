package com.modelmatrix4j.tool;

import com.modelmatrix4j.core.scenario.Scenario;

/** Performs one physical application/model invocation and returns its tool-call evidence. */
@FunctionalInterface
public interface ToolAdapter {
    /**
     * Executes one tool-aware physical invocation for the scenario.
     *
     * @param scenario scenario being executed
     * @return normal output and ordered tool-call evidence from the same invocation
     * @throws Exception when invocation or tool integration fails
     */
    ToolInvocation invoke(Scenario scenario) throws Exception;
}
