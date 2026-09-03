package com.modelmatrix4j.rag;

import com.modelmatrix4j.core.scenario.Scenario;

/** Performs one physical model/application invocation and returns retrieval evidence from that same invocation. */
@FunctionalInterface
public interface RetrievalAdapter {
    /**
     * @param scenario provider-neutral scenario for this invocation
     * @return generation output and retrieval evidence produced by the same physical invocation
     * @throws Exception when model/application execution fails
     */
    RetrievalInvocation invoke(Scenario scenario) throws Exception;
}
