package com.modelmatrix4j.core.model;

import com.modelmatrix4j.core.scenario.Scenario;

/** Provider-neutral outbound port for one model invocation. Returns the raw provider text. */
@FunctionalInterface
public interface ModelAdapter {
    /**
     * Invokes this adapter once for the supplied scenario.
     *
     * <p>Core does not retry calls. Implementations should cooperate with thread interruption so
     * timeout and cancellation can promptly release adapter-owned resources. Provider-specific
     * behavior must be translated to this contract; unavailability is reported with
     * {@link ModelUnavailableException}, cancellation with an interruption or cancellation
     * exception, and other failures may be thrown normally.
     *
     * <p>Each declared {@link ModelUnderTest} is an independent execution target. Distinct targets
     * may be invoked concurrently even when they reference the same adapter instance, so callers
     * sharing an adapter are responsible for making that adapter safe for concurrent invocation.
     */
    String invoke(Scenario scenario) throws Exception;
}
