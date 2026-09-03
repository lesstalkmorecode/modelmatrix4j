package com.modelmatrix4j.mcp;

import java.util.List;
import java.util.Objects;

/**
 * Normal output and ordered MCP tool evidence produced by the same physical invocation. Incidental
 * rendering omits both output and tool argument payloads.
 */
public record McpInvocation(String output, List<McpToolInteraction> tools) {
    /** @throws NullPointerException if output, tools, or a tool interaction is {@code null} */
    public McpInvocation {
        output = Objects.requireNonNull(output, "output");
        tools = List.copyOf(Objects.requireNonNull(tools, "tools"));
        if (tools.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("tools must not contain null");
        }
    }

    @Override
    public String toString() {
        return "McpInvocation[output=<hidden>, toolCount=" + tools.size() + "]";
    }
}
