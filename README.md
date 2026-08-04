# CouchWeave

CouchWeave is an independent Java project aiming to provide repository-style,
Spring Data-compatible access to Apache CouchDB.

Visit [couchweave.marcelorodrigo.com](https://couchweave.marcelorodrigo.com/)
for the project overview, roadmap, design rationale, and documentation status.

> [!IMPORTANT]
> CouchWeave is in its initial design phase. It does not yet publish usable
> artifacts or provide a working CouchDB integration.

## Vision

CouchWeave will build on Spring Data Commons to offer a familiar repository
programming model while preserving CouchDB-specific concepts and behavior.

## Why CouchWeave?

The Java ecosystem already offers capable CouchDB clients and earlier
repository-style integrations. CouchWeave addresses a different gap: a current
Spring Data store module that combines repository and mapping conventions with
CouchDB's native revision, query, indexing, and pagination semantics.

The [Why CouchWeave Exists](https://couchweave.marcelorodrigo.com/why/) page
describes the projects that informed this direction, the problems each one
solves, and the integration work that remains. The comparison is intended to
recognize prior work and make CouchWeave's scope explicit, not to rank or
criticize other projects.

The planned capabilities include:

- CRUD repositories for CouchDB documents
- Object mapping for Java types, document IDs, and document revisions
- Derived repository queries translated to CouchDB Mango selectors
- Pagination and sorting where CouchDB can support them predictably
- Optimistic-locking semantics based on CouchDB revisions
- Spring Boot auto-configuration and a third-party starter
- Explicit documentation of supported and unsupported Spring Data features

## Design principles

- Preserve CouchDB semantics instead of hiding meaningful datastore behavior.
- Keep the core integration usable independently of Spring Boot.
- Fail clearly when a Spring Data operation cannot be represented faithfully.
- Maintain an explicit compatibility matrix for Java, Spring Data, Spring Boot,
  and CouchDB versions.
- Test repository behavior against real CouchDB instances.

## Modules

The Maven reactor currently contains:

| Module | Responsibility |
| --- | --- |
| `couchweave-core` | CouchDB mapping, operations, repository implementation, and query support |
| `couchweave-spring-boot` | Spring Boot auto-configuration |
| `couchweave-spring-boot-starter` | Opinionated dependency entry point for Spring Boot applications |
| `samples/couchweave-sample` | Non-publishable sample used to verify the complete reactor and test lifecycle |
| `build-support/couchdb-test-support` | Non-published CouchDB Testcontainers fixture for datastore integration tests |

## Project status

The repository contains the initial multi-module project skeleton at version
`0.0.1-SNAPSHOT`. The build produces the core, Spring Boot integration, and
starter artifacts, but these modules do not yet provide CouchDB behavior or a
public Java API.

The [compatibility policy](docs/compatibility.md) records the initial Java,
Spring Data, Spring Boot, and CouchDB version contract. The
[feature-support policy](docs/supported-features.md) distinguishes implemented,
planned, deferred, and unsupported behavior.

## Building

The build requires JDK 21 or newer. The Maven Wrapper downloads and runs the
pinned Maven version, so a separate Maven installation is not required.

On macOS or Linux:

```shell
./mvnw verify
```

On Windows:

```bat
mvnw.cmd verify
```

To build the sample and every upstream module it depends on:

```shell
./mvnw -pl samples/couchweave-sample -am verify
```

## Testing conventions

The parent build manages JUnit Jupiter 6, AssertJ, Mockito, Surefire, Failsafe,
and JaCoCo for every module. Add the test dependencies a module uses without
specifying versions; the parent test toolchain supplies compatible versions.

- Unit tests end in `Test` and run in Maven's `test` phase through Surefire.
- Integration tests end in `IT`, begin with `IT`, or end in `ITCase`; Failsafe
  runs them during `integration-test` and verifies them in `verify`.
- Name test methods `should...`, give every test a sentence-case `@DisplayName`,
  organize test bodies with `// given`, `// when`, and `// then`, and use
  AssertJ for assertions.
- Unit tests instantiate the subject directly and do not load a Spring context.
  Use Mockito only for external collaborators.

JaCoCo enforces a minimum 80% line-coverage ratio for modules that produce
coverage data and writes a reactor aggregate report to
`build-support/coverage/target/site/jacoco-aggregate/`.

### CouchDB integration tests

Datastore-facing tests can depend on `couchweave-couchdb-test-support` with
`test` scope, annotate the test class with `@CouchDbIntegrationTest`, and accept
`CouchDbTestDatabase` as a constructor or test-method parameter. The harness
starts `couchdb:3.5` on a dynamic port, provides its admin credentials, and
creates then removes an isolated database for each test class.

Docker or Podman must be running to execute these tests. Local runs skip them
with an actionable message when no container runtime is available; CI fails in
that case so the real-CouchDB smoke test cannot be skipped silently.

The following opt-in fixtures verify the build policy without affecting the
normal reactor:

```shell
./mvnw -Ptest-discovery-fixture verify
./mvnw -Pcoverage-failure-fixture verify # expected to fail JaCoCo coverage
```

## Contributing

Run the complete Maven verification build before submitting a change. Further
contribution conventions will be documented as the implementation develops.

## License

CouchWeave is available under the [Apache License 2.0](LICENSE).

## Trademarks

CouchWeave is an independent project and is not affiliated with or endorsed by
Broadcom, the Spring team, the Apache Software Foundation, or the Apache CouchDB
project.

Spring is a trademark of Broadcom Inc. and/or its subsidiaries. Apache CouchDB,
CouchDB, and Apache are trademarks or registered trademarks of the Apache
Software Foundation in the United States and/or other countries.
