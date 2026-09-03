package com.modelmatrix4j.core.model;

import com.modelmatrix4j.core.scenario.Scenario;

/** Provider-neutral outbound port for one physical model invocation. */
@FunctionalInterface
public interface ModelAdapter {
    /**
     * Invokes this adapter once for the supplied scenario and returns its application-visible text.
     *
     * <p>Core does not retry calls. Implementations should cooperate with thread interruption so
     * timeout and cancellation can promptly release adapter-owned resources. Provider-specific
     * behavior must be translated to this contract; unavailability is reported with
     * {@link ModelUnavailableException}, cancellation with an interruption or cancellation
     * exception, and other failures may be thrown normally.</p>
     *
     * <p>Each declared {@link ModelUnderTest} is an independent execution target. Distinct targets
     * may be invoked concurrently even when they reference the same adapter instance, so callers
     * sharing an adapter are responsible for making that adapter safe for concurrent invocation.</p>
     *
     * @param scenario scenario being executed
     * @return application-visible text produced by the invocation; must not be {@code null}
     * @throws ModelUnavailableException when the configured model/provider is unavailable
     * @throws Exception for other invocation failures
     */
    String invoke(Scenario scenario) throws Exception;
}
