# ModelMatrix4J
Compatibility and regression testing framework for Spring AI applications across models and providers.

## Build

The M1 foundation targets Java 25 and uses Maven Wrapper for reproducible builds. Default verification is deterministic and requires no external runtime services, model providers, databases, Docker daemon, Ollama, or cloud credentials. Normal Maven dependency resolution may access configured artifact repositories.

```text
./mvnw -B verify
```

On Windows, use `mvnw.cmd -B verify`.

The parent POM centralizes dependency versions and compiler settings. Maven Enforcer checks the Java 25 baseline and dependency convergence repository-wide; a core-only dependency boundary prevents framework and provider dependencies from entering `modelmatrix-core`.

The two initial modules are:

- `modelmatrix-core`: framework- and provider-free foundation.
- `modelmatrix-junit`: JUnit Jupiter integration boundary, depending inward on core.

Spring AI, Ollama, MCP, PostgreSQL, provider SDKs, and external-service tests are intentionally deferred to later milestones.
