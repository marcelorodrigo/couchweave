# CouchWeave

CouchWeave is an independent Java project aiming to provide repository-style,
Spring Data-compatible access to Apache CouchDB.

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

The [Why CouchWeave Exists](docs/why-couchweave.md) page describes the projects
that informed this direction, the problems each one solves, and the integration
work that remains. The comparison is intended to recognize prior work and make
CouchWeave's scope explicit, not to rank or criticize other projects.

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

## Project status

The repository contains the initial multi-module project skeleton at version
`0.0.1-SNAPSHOT`. The build produces the core, Spring Boot integration, and
starter artifacts, but these modules do not yet provide CouchDB behavior or a
public Java API.

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
