package com.modelmatrix4j.springai;

import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.rag.RetrievalAdapter;
import com.modelmatrix4j.rag.RetrievalInvocation;
import com.modelmatrix4j.rag.RetrievedDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;

/**
 * Observes retrieval evidence already produced by a Spring AI {@link ChatClient} advisor chain.
 * This adapter does not own retrieval, query rewriting, prompt augmentation, or vector-store access.
 *
 * <p>The supplied identity and citation functions define provider-neutral evidence normalization.
 * Runtime failures while normalizing evidence are classified as invalid retrieval evidence while
 * preserving completed generation output; they are not reclassified as core execution failures.
 * These functions should therefore be deterministic and side-effect free.</p>
 */
public final class SpringAiRetrievalAdapter implements RetrievalAdapter {
    private final Function<Scenario, ChatClientResponse> invocation;
    private final String documentsContextKey;
    private final Function<Document, String> documentIdentity;
    private final Function<Document, Optional<String>> citation;

    /**
     * Uses no citation evidence.
     *
     * @param chatClient Spring AI client whose advisor context contains retrieved documents
     * @param documentsContextKey context key containing a list of Spring AI documents
     * @param documentIdentity stable logical identity mapping used for compatibility comparison
     */
    public SpringAiRetrievalAdapter(
            ChatClient chatClient,
            String documentsContextKey,
            Function<Document, String> documentIdentity
    ) {
        this(chatInvocation(chatClient), documentsContextKey, documentIdentity, ignored -> Optional.empty());
    }

    /**
     * @param chatClient Spring AI client whose advisor context contains retrieved documents
     * @param documentsContextKey context key containing a list of Spring AI documents
     * @param documentIdentity stable logical identity mapping used for compatibility comparison
     * @param citation optional citation mapping; citations do not participate in default compatibility
     */
    public SpringAiRetrievalAdapter(
            ChatClient chatClient,
            String documentsContextKey,
            Function<Document, String> documentIdentity,
            Function<Document, Optional<String>> citation
    ) {
        this(chatInvocation(chatClient), documentsContextKey, documentIdentity, citation);
    }

    SpringAiRetrievalAdapter(
            Function<Scenario, ChatClientResponse> invocation,
            String documentsContextKey,
            Function<Document, String> documentIdentity,
            Function<Document, Optional<String>> citation
    ) {
        this.invocation = Objects.requireNonNull(invocation, "invocation");
        this.documentsContextKey = requireText(documentsContextKey, "documentsContextKey");
        this.documentIdentity = Objects.requireNonNull(documentIdentity, "documentIdentity");
        this.citation = Objects.requireNonNull(citation, "citation");
    }

    /**
     * Executes one ChatClient turn and normalizes retrieval evidence from its advisor context.
     * Missing, malformed, or normalization-failing document evidence produces
     * {@link RetrievalInvocation.EvidenceStatus#INVALID} without discarding completed model output.
     */
    @Override
    public RetrievalInvocation invoke(Scenario scenario) {
        ChatClientResponse response = Objects.requireNonNull(invocation.apply(scenario), "response");
        String output = output(response);
        Object rawDocuments = response.context().get(documentsContextKey);
        if (!(rawDocuments instanceof List<?> values)) {
            return RetrievalInvocation.invalidEvidence(output);
        }

        try {
            List<RetrievedDocument> documents = new ArrayList<>(values.size());
            for (Object value : values) {
                if (!(value instanceof Document document)) {
                    return RetrievalInvocation.invalidEvidence(output);
                }
                documents.add(new RetrievedDocument(
                        documentIdentity.apply(document),
                        Objects.requireNonNull(citation.apply(document), "citation")));
            }
            return new RetrievalInvocation(output, documents);
        } catch (RuntimeException normalizationFailure) {
            return RetrievalInvocation.invalidEvidence(output);
        }
    }

    private static Function<Scenario, ChatClientResponse> chatInvocation(ChatClient chatClient) {
        Objects.requireNonNull(chatClient, "chatClient");
        return scenario -> chatClient.prompt(scenario.input()).call().chatClientResponse();
    }

    private static String output(ChatClientResponse response) {
        ChatResponse chatResponse = Objects.requireNonNull(response.chatResponse(), "chatResponse");
        Generation result = Objects.requireNonNull(chatResponse.getResult(), "chatResponse.result");
        return Objects.requireNonNull(result.getOutput().getText(), "chatResponse.output.text");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
