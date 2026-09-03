package com.modelmatrix4j.springai;

import com.modelmatrix4j.mcp.McpToolInteraction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Invocation-scoped callback decorator used by {@link SpringAiMcpToolAdapter}.
 *
 * <p>An interaction is recorded immediately before delegating the tool call, so it represents an
 * attempted application-visible invocation. If the delegate fails, the surrounding model execution
 * fails and {@code McpExecution} does not expose that evidence as a completed-run observation.</p>
 */
final class SpringAiMcpToolObserver {
    private final List<McpToolInteraction> interactions = new ArrayList<>();

    ToolCallback[] observe(List<ToolCallback> callbacks) {
        Objects.requireNonNull(callbacks, "callbacks");
        ToolCallback[] observed = new ToolCallback[callbacks.size()];
        for (int index = 0; index < callbacks.size(); index++) {
            observed[index] = new ObservedToolCallback(callbacks.get(index));
        }
        return observed;
    }

    synchronized List<McpToolInteraction> interactions() {
        return List.copyOf(interactions);
    }

    private synchronized void record(ToolCallback callback, String toolInput) {
        interactions.add(new McpToolInteraction(
                callback.getToolDefinition().name(),
                Objects.requireNonNull(toolInput, "toolInput")));
    }

    private final class ObservedToolCallback implements ToolCallback {
        private final ToolCallback delegate;

        private ObservedToolCallback(ToolCallback delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            record(delegate, toolInput);
            return delegate.call(toolInput);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            record(delegate, toolInput);
            return delegate.call(toolInput, toolContext);
        }
    }
}
