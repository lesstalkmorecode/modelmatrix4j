package com.modelmatrix4j.springai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.rag.RetrievalEvaluator;
import com.modelmatrix4j.rag.RetrievalExecution;
import com.modelmatrix4j.rag.RetrievalModel;
import com.modelmatrix4j.rag.RetrievalResult;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIdType;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Execution(ExecutionMode.SAME_THREAD)
@Testcontainers
class PgVectorRetrievalIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("modelmatrix")
            .withUsername("modelmatrix")
            .withPassword("modelmatrix");

    private PgVectorStore vectorStore;

    @BeforeEach
    void prepareCorpus() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP TABLE IF EXISTS vector_store");
        vectorStore = PgVectorStore.builder(jdbc, new DeterministicEmbeddingModel())
                .dimensions(3)
                .idType(PgIdType.TEXT)
                .indexType(PgIndexType.NONE)
                .initializeSchema(true)
                .build();
        vectorStore.afterPropertiesSet();
        vectorStore.add(List.of(
                document("provider-java", "Java virtual threads improve concurrent application scalability", "java-threads", "java"),
                document("provider-spring", "Spring AI supports tool calling and model applications for concurrent systems", "spring-ai-tools", "spring"),
                document("provider-db", "PostgreSQL stores relational application data", "postgres-data", "database")));
    }

    @Test
    void realPgVectorRetrievalFlowsThroughSpringAdvisorAndCapabilityEvaluation() {
        var retrieval = evaluate(
                adapter(search(1, null)),
                adapter(search(1, null)),
                "virtual threads concurrency");

        assertEquals(RetrievalResult.Status.COMPATIBLE, retrieval.result().status());
        assertEquals(List.of("java-threads", "java-threads"), firstDocumentIds(retrieval));
        assertEquals(List.of("answer", "answer"), retrieval.outputs());
    }

    @Test
    void realCrossConfigurationRetrievalDifferenceIsMismatch() {
        var retrieval = evaluate(
                adapter(search(1, null)),
                adapter(search(2, null)),
                "virtual threads concurrency");

        assertEquals(RetrievalResult.Status.MISMATCH, retrieval.result().status());
        assertEquals(List.of(1, 2), retrieval.documentCounts());
    }

    @Test
    void filterOutcomeIsVisibleAsRetrievalMismatch() {
        var retrieval = evaluate(
                adapter(search(1, null)),
                adapter(search(1, "topic == 'spring'")),
                "virtual threads concurrency");

        assertEquals(RetrievalResult.Status.MISMATCH, retrieval.result().status());
        assertEquals(List.of("java-threads", "spring-ai-tools"), firstDocumentIds(retrieval));
    }

    @Test
    void filterCanProduceValidNoResultEvidence() {
        var retrieval = evaluate(
                adapter(search(1, "topic == 'missing'")),
                adapter(search(1, "topic == 'missing'")),
                "virtual threads concurrency");

        assertEquals(RetrievalResult.Status.COMPATIBLE, retrieval.result().status());
        assertEquals(List.of(0, 0), retrieval.documentCounts());
    }

    private Evaluation evaluate(
            SpringAiRetrievalAdapter baseline,
            SpringAiRetrievalAdapter candidate,
            String query
    ) {
        var prepared = RetrievalExecution.prepare(List.of(
                new RetrievalModel(new ModelDescriptor("baseline"), baseline),
                new RetrievalModel(new ModelDescriptor("candidate"), candidate)));
        var core = ModelMatrix.builder().models(prepared.models()).build()
                .run(new Scenario("pgvector-rag", query));
        var observations = prepared.observations(core);
        return new Evaluation(
                new RetrievalEvaluator().evaluate(observations),
                observations.stream().map(observation -> observation.documents().size()).toList(),
                observations.stream()
                        .filter(observation -> !observation.documents().isEmpty())
                        .map(observation -> observation.documents().getFirst().documentId())
                        .toList(),
                core.runs().stream().map(run -> run.output()).toList());
    }

    private SpringAiRetrievalAdapter adapter(SearchRequest searchRequest) {
        var advisor = QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build();
        ChatModel chatModel = prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage("answer"))));
        ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(advisor).build();
        return new SpringAiRetrievalAdapter(
                chatClient,
                QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS,
                document -> document.getMetadata().get("logicalId").toString(),
                document -> Optional.of(document.getMetadata().get("source").toString()));
    }

    private static SearchRequest search(int topK, String filter) {
        var builder = SearchRequest.builder().topK(topK).similarityThresholdAll();
        if (filter != null) {
            builder.filterExpression(filter);
        }
        return builder.build();
    }

    private static List<String> firstDocumentIds(Evaluation evaluation) {
        return evaluation.firstDocumentIds();
    }

    private static Document document(String providerId, String text, String logicalId, String topic) {
        return new Document(providerId, text, Map.of(
                "logicalId", logicalId,
                "source", logicalId + ".md",
                "topic", topic));
    }

    private record Evaluation(
            RetrievalResult result,
            List<Integer> documentCounts,
            List<String> firstDocumentIds,
            List<String> outputs
    ) {}

    private static final class DeterministicEmbeddingModel implements EmbeddingModel {
        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            var embeddings = new java.util.ArrayList<Embedding>();
            for (int index = 0; index < request.getInstructions().size(); index++) {
                embeddings.add(new Embedding(vector(request.getInstructions().get(index)), index));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return vector(document.getText());
        }

        @Override
        public int dimensions() {
            return 3;
        }

        private static float[] vector(String text) {
            String normalized = text.toLowerCase(Locale.ROOT);
            return new float[] {
                    containsAny(normalized, "java", "virtual", "thread", "concurr") ? 1.0f : 0.0f,
                    containsAny(normalized, "spring", "tool", "model") ? 1.0f : 0.0f,
                    containsAny(normalized, "postgres", "database", "relational") ? 1.0f : 0.0f
            };
        }

        private static boolean containsAny(String text, String... terms) {
            for (String term : terms) {
                if (text.contains(term)) {
                    return true;
                }
            }
            return false;
        }
    }
}
