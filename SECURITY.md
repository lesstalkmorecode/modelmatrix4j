# Security Policy

ModelMatrix4J is a public reference library. This document records security boundaries for the repository and its generated artifacts; it does not define a support or maintenance schedule.

## Sensitive information

Do not place credentials, API keys, access tokens, private prompts, customer data, provider-native payloads, model outputs, retrieved private content, tool arguments/results, MCP arguments, or other sensitive runtime data in public repository content.

If you independently investigate or fork the project, keep any sensitive reproduction data outside public commits, logs, fixtures, and CI artifacts unless it has been deliberately sanitized.

## Reporting boundary

Persistent reporting is intentionally narrower than transient runtime results. The default durable report schema does not persist model output, diagnostics, raw provider payloads, structured payloads, tool arguments/results, retrieved content, or MCP arguments.

A change that widens the persisted report surface requires an explicit schema and security review. The durable report contract is documented in [`docs/REPORT_SCHEMA.md`](docs/REPORT_SCHEMA.md), and the architectural rationale is summarized in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Application responsibility

ModelMatrix4J executes user-configured adapters and integrations. Applications remain responsible for provider credentials, network policy, external-service permissions, model/tool/retrieval-system security, and the handling of any sensitive data supplied to those integrations.

The repository does not make provider credentials or external services part of the default deterministic verification path.
