# Bemo Shared Framework

Reusable, app-agnostic infrastructure for the Bemo ERP backend. Built for **Spring Boot 4**
and **Java 17**, packaged as a standalone Gradle library (`com.bemo:shared`) that the
`be` application consumes via a composite build.

## What's inside

| Area | Package | Highlights |
|---|---|---|
| Observability | `com.bemo.shared.filter` | `TracingFilter` (correlation ids), `MdcFilter` (tenant/user MDC propagation), `ServerLoggingFilter` (structured JSON access log), `RequestPerformanceLoggingFilter` (timing + Micrometer) |
| Safe logging | `com.bemo.shared.logging` | `SafeLoggingUtil`, `Maskers` (secrets/URL masking), `JsonLoggingUtil` (one-line JSON records), `MdcDataProvider` |
| Performance | `com.bemo.shared.aspect` + `config` | `@MeasureMethodLatency` aspect, cache + schedule latency aspects, `PerformanceLoggingProperties` |
| HTTP support | `com.bemo.shared.http` | `ClientLoggingInterceptor` (WebClient), `LogLine`/`RequestLogLine`/`ResponseLogLine`, `MultipleReadRequestWrapper` (replayable bodies) |
| Concurrency | `com.bemo.shared.concurrency` | `AsyncContextSwitcher` (MDC propagation across threads), `ContextCopyTaskDecorator`, `ExecutorsFactory`, `DefaultAsyncConfigurer`, context-copying scheduled pool |
| Caching | `com.bemo.shared.cache` | Redis cache manager with JSON + `java.time` serialization |
| Errors | `com.bemo.shared.exception` | `BemoException`/`BemoError`, `BemoErrorType`, `BadInputException` |
| Security | `com.bemo.shared.security` | `KeyUtils` (PEM/Base64 key loading, no BouncyCastle) |
| Data | `com.bemo.shared.db` | `QueryLoggingStatementInspector` (SQL logging) |
| Validation | `com.bemo.shared.validation` | `HTMLValidator` (jsoup allow-list sanitisation), `XSSInputValidator` |
| Utilities | `com.bemo.shared.util` | `Assertion`, `NumberUtils`, `DateUtils`, `RandomString`, `Suppress` |

## Activating it in an app

Add the dependency and annotate your `@SpringBootApplication`:

```java
@SpringBootApplication
@EnablePerformanceMetricsLogging
public class App { ... }
```

This imports `PerformanceMetricsLoggingConfiguration` (filters + latency aspects) and loads
`shared.properties` defaults. Optional extras:

```java
@Configuration
@EnableConfigurationProperties(ConcurrencyProperties.class)
public class AppConfig {
    @Bean
    public DefaultAsyncConfigurer sharedAsyncConfigurer(ConcurrencyProperties p) {
        return new DefaultAsyncConfigurer(p);   // enables @Async with MDC propagation
    }
}
```

- Redis caching: `shared.cache.redis.enabled=true`
- SQL logging: `shared.logging.sql.enabled=true` + register `QueryLoggingStatementInspector`
- XSS filter: `@EnableXSSValidation` (opt methods out with `@IgnoreXSSValidation`)

## Naming conventions

Names were cleaned up while porting from the legacy codebase: `Foo*` prefixes and `New*`
legacy names are dropped, `Framework*`/domain-appropriate names are used instead
(e.g. `FooExecutorsFactory -> ExecutorsFactory`, `NewServerLoggingFilter -> ServerLoggingFilter`,
`FooRequestAttribute -> MetricTypes`, `FooRequestHeader -> CorrelationHeaders`).

## Build & test

```bash
gradle build   # or: (cd ../be && ./gradlew :shared:build)
```

Requirements: JDK 17+ with `--release 17`; dependencies resolved from Maven Central.
