package com.modelmatrix4j.report;

import java.util.Objects;

/**
 * Writes deterministic line-oriented text from the persistence-safe report projection. Identifier
 * control characters and line separators are escaped so untrusted identifiers cannot inject
 * additional CI/log lines. Model output and diagnostics are outside {@link CompatibilityReport}.
 */
public final class TextReportWriter {
    public TextReportWriter() {
    }

    public String write(CompatibilityReport report) {
        Objects.requireNonNull(report, "report");
        StringBuilder text = new StringBuilder()
                .append("ModelMatrix report v").append(escape(report.schemaVersion()))
                .append(" — ").append(report.status()).append('\n');
        for (RunReport run : report.runs()) {
            text.append(escape(run.runId()))
                    .append(" | scenario=").append(escape(run.scenarioId()))
                    .append(" | configuration=").append(escape(run.configurationId()))
                    .append(" | repetition=").append(run.repetition())
                    .append(" | status=").append(run.status())
                    .append(" | durationNanos=").append(run.durationNanos())
                    .append('\n');
        }
        return text.toString();
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            switch (codePoint) {
                case '\\' -> escaped.append("\\\\");
                case '\r' -> escaped.append("\\r");
                case '\n' -> escaped.append("\\n");
                case '\t' -> escaped.append("\\t");
                case 0x2028 -> escaped.append("\\u2028");
                case 0x2029 -> escaped.append("\\u2029");
                default -> {
                    if (Character.isISOControl(codePoint)) {
                        escaped.append(String.format("\\u%04X", codePoint));
                    } else {
                        escaped.appendCodePoint(codePoint);
                    }
                }
            }
        });
        return escaped.toString();
    }
}
