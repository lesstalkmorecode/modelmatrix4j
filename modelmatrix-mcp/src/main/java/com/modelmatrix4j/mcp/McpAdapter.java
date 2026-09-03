package com.modelmatrix4j.mcp;

import com.modelmatrix4j.core.scenario.Scenario;

/** Performs one physical model/application invocation and returns MCP evidence from that same invocation. */
@FunctionalInterface
public interface McpAdapter {
    /**
     * @param scenario provider-neutral scenario for this invocation
     * @return normal output and ordered MCP tool evidence from the same physical invocation
     * @throws Exception when model/application or MCP-backed execution fails
     */
    McpInvocation invoke(Scenario scenario) throws Exception;
}
