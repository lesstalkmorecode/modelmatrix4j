package com.modelmatrix4j.rag;

import com.modelmatrix4j.core.model.ModelDescriptor;
import java.util.Objects;

/** Retrieval-aware model configuration adapted into the core execution lifecycle. */
public record RetrievalModel(ModelDescriptor descriptor, RetrievalAdapter adapter) {
    public RetrievalModel {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(adapter, "adapter");
    }
}
