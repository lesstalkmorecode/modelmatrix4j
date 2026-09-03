package com.modelmatrix4j.springai;

import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.mcp.McpAdapter;
import com.modelmatrix4j.mcp.McpInvocation;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

/**
 * Executes one Spring AI {@link ChatClient} turn and captures supplied tool callbacks invoked during
 * that same physical execution. No second model invocation is made for MCP evaluation.
 *
 * <p>The callbacks may be backed by MCP clients; they retain ownership of transport, session, and
 * runtime behavior. Evidence is isolated per invocation so it cannot leak across repetitions or
 * concurrent runs.</p>
 */
public final class SpringAiMcpToolAdapter implements McpAdapter {
    private final ChatClient chatClient;
    private final List<ToolCallback> toolCallbacks;

    /**
     * @throws NullPointerException if the client, callback array, or a callback is {@code null}
     */
    public SpringAiMcpToolAdapter(ChatClient chatClient, ToolCallback... toolCallbacks) {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient");
        Objects.requireNonNull(toolCallbacks, "toolCallbacks");
        this.toolCallbacks = List.of(toolCallbacks.clone());
    }

    /** Returns model output and ordered observed callback interactions from one ChatClient turn. */
    @Override
    public McpInvocation invoke(Scenario scenario) {
        Objects.requireNonNull(scenario, "scenario");
        SpringAiMcpToolObserver observer = new SpringAiMcpToolObserver();
        ToolCallback[] observedCallbacks = observer.observe(toolCallbacks);

        String output = Objects.requireNonNull(
                chatClient.prompt()
                        .user(scenario.input())
                        .tools((Object[]) observedCallbacks)
                        .call()
                        .content(),
                "chatClient content");

        return new McpInvocation(output, observer.interactions());
    }
}