package com.modelmatrix4j.springai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.mcp.McpInvocation;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import reactor.core.publisher.Mono;

class McpInProcessIntegrationIT {

    @Test
    void capturesToolEvidenceFromARealMcpClientServerRoundTrip() throws IOException {
        McpJsonMapper mapper = McpJsonDefaults.getMapper();
        PipedInputStream serverInput = new PipedInputStream(64 * 1024);
        PipedOutputStream clientOutput = new PipedOutputStream(serverInput);
        PipedInputStream clientInput = new PipedInputStream(64 * 1024);
        PipedOutputStream serverOutput = new PipedOutputStream(clientInput);

        StdioServerTransportProvider serverTransport =
                new StdioServerTransportProvider(mapper, serverInput, serverOutput);
        McpSyncServer server = McpServer.sync(serverTransport)
                .serverInfo("modelmatrix-test-mcp", "1.0.0")
                .toolCall(
                        McpSchema.Tool.builder("weather", Map.of(
                                        "type", "object",
                                        "properties", Map.of("city", Map.of("type", "string")),
                                        "required", List.of("city"),
                                        "additionalProperties", false))
                                .description("Returns deterministic weather for a city")
                                .build(),
                        (exchange, request) -> McpSchema.CallToolResult.builder()
                                .content(List.of(McpSchema.TextContent.builder(
                                                "weather:" + request.arguments().get("city") + ":sunny")
                                        .build()))
                                .build())
                .build();

        InProcessClientTransport clientTransport =
                new InProcessClientTransport(mapper, clientInput, clientOutput);
        McpSyncClient client = McpClient.sync(clientTransport)
                .requestTimeout(Duration.ofSeconds(10))
                .build();

        try {
            client.initialize();
            var callbacks = new SyncMcpToolCallbackProvider(List.of(client)).getToolCallbacks();
            assertEquals(1, callbacks.length);
            assertEquals("weather", callbacks[0].getToolDefinition().name());

            AtomicInteger modelCalls = new AtomicInteger();
            ChatModel model = toolCallingModel(call -> call == 0
                    ? responseWithTool("1", "weather", "{\"city\":\"Berlin\"}")
                    : finalResponse("sunny"), modelCalls);
            SpringAiMcpToolAdapter adapter = new SpringAiMcpToolAdapter(ChatClient.create(model), callbacks);

            McpInvocation invocation = adapter.invoke(new Scenario("mcp-in-process", "weather in Berlin"));

            assertEquals("sunny", invocation.output());
            assertEquals(1, invocation.tools().size());
            assertEquals("weather", invocation.tools().getFirst().toolId());
            assertEquals("{\"city\":\"Berlin\"}", invocation.tools().getFirst().argumentsJson());
            assertEquals(2, modelCalls.get());
        } finally {
            client.closeGracefully();
            server.closeGracefully();
        }
    }

    private static ChatModel toolCallingModel(IntFunction<ChatResponse> response, AtomicInteger calls) {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return response.apply(calls.getAndIncrement());
            }

            @Override
            public ChatOptions getOptions() {
                return DefaultToolCallingChatOptions.builder().build();
            }
        };
    }

    private static ChatResponse responseWithTool(String id, String name, String arguments) {
        AssistantMessage message = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments)))
                .build();
        return new ChatResponse(List.of(new Generation(message)));
    }

    private static ChatResponse finalResponse(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    private static final class InProcessClientTransport implements McpClientTransport {
        private final McpJsonMapper mapper;
        private final BufferedReader input;
        private final OutputStream output;
        private final ExecutorService readerExecutor;
        private final AtomicBoolean closed = new AtomicBoolean();
        private volatile Consumer<Throwable> exceptionHandler = ignored -> { };

        private InProcessClientTransport(McpJsonMapper mapper, InputStream input, OutputStream output) {
            this.mapper = Objects.requireNonNull(mapper, "mapper");
            this.input = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(input, "input"), StandardCharsets.UTF_8));
            this.output = Objects.requireNonNull(output, "output");
            this.readerExecutor = Executors.newSingleThreadExecutor(task -> {
                Thread thread = new Thread(task, "mcp-in-process-client");
                thread.setDaemon(true);
                return thread;
            });
        }

        @Override
        public Mono<Void> connect(
                Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
            Objects.requireNonNull(handler, "handler");
            return Mono.fromRunnable(() -> readerExecutor.execute(() -> readLoop(handler)));
        }

        @Override
        public void setExceptionHandler(Consumer<Throwable> handler) {
            this.exceptionHandler = Objects.requireNonNull(handler, "handler");
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            Objects.requireNonNull(message, "message");
            return Mono.fromRunnable(() -> {
                try {
                    String json = mapper.writeValueAsString(message);
                    synchronized (output) {
                        output.write(json.getBytes(StandardCharsets.UTF_8));
                        output.write('\n');
                        output.flush();
                    }
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return mapper.convertValue(data, typeRef);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(() -> {
                if (!closed.compareAndSet(false, true)) {
                    return;
                }
                readerExecutor.shutdownNow();
                try {
                    input.close();
                } catch (IOException ignored) {
                    // Closing a test pipe is best effort.
                }
                try {
                    output.close();
                } catch (IOException ignored) {
                    // Closing a test pipe is best effort.
                }
            });
        }

        private void readLoop(
                Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
            try {
                String line;
                while (!closed.get() && (line = input.readLine()) != null) {
                    McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(mapper, line);
                    handler.apply(Mono.just(message)).subscribe(ignored -> { }, exceptionHandler);
                }
            } catch (IOException | RuntimeException exception) {
                if (!closed.get()) {
                    exceptionHandler.accept(exception);
                }
            }
        }
    }
}
