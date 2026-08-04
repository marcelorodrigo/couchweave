# Why CouchWeave Exists

Java developers can already connect to CouchDB with capable libraries. The
remaining gap is not basic HTTP access or document CRUD; it is a current Spring
Data integration that combines familiar repository conventions with CouchDB's
revision, query, indexing, and pagination semantics. Without that layer, Spring
applications either work directly with a client API or adopt an abstraction
that covers only part of the Spring Data programming model.

CouchWeave addresses that integration gap. It is intended for Spring developers
who want repository-style data access without treating CouchDB like a relational
database or hiding behavior that matters to correctness.

> [!NOTE]
> This assessment reflects the public documentation and source code available on
> 4 August 2026. The projects discussed here may continue to evolve. CouchWeave
> is independent of these projects and recognizes the work they contributed to
> the Java and CouchDB ecosystems.

## Spring Data compatibility requires more than a repository interface

A Spring Data store module connects datastore behavior to several related
framework contracts. For CouchWeave, compatibility means addressing these
areas together:

| Area | Required integration |
| --- | --- |
| Repositories | Repository discovery, factory support, query lookup, fragments, and multi-store identification |
| Mapping | Persistent entity metadata, IDs, revisions, conversions, object creation, and lifecycle callbacks |
| Queries | Derived method parsing, Mango translation, result processing, and explicit unsupported operations |
| CouchDB semantics | Revision conflicts, per-document bulk results, index constraints, and bookmark-based continuation |
| Spring integration | Consistent exception translation and operation contracts |
| Spring Boot | Conditional auto-configuration, configuration properties, and starter packaging |
| Compatibility | A tested Java, Spring Data, Spring Boot, and CouchDB version matrix |

Implementing `CrudRepository` can provide a useful, familiar API. A complete
store module also needs the infrastructure behind that API and datastore-aware
definitions of save, query, paging, and failure behavior. Spring Data exposes
extension points such as
[`RepositoryFactorySupport`](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/repository/core/support/RepositoryFactorySupport.html)
and
[`MappingContext`](https://docs.spring.io/spring-data/commons/reference/api/java/org/springframework/data/mapping/context/MappingContext.html)
for those responsibilities.

## Existing projects solve valuable parts of the problem

The projects below have different goals and come from different generations of
the Java and Spring ecosystems. Comparing their intended scope makes the
remaining integration work clearer without treating unlike projects as direct
competitors.

| Project | Intended scope | Useful contribution | Capability outside that scope or baseline |
| --- | --- | --- | --- |
| [IBM Cloudant Java SDK](https://github.com/IBM/cloudant-java-sdk) | A supported Java client for IBM Cloudant and Apache CouchDB data operations | Broad protocol coverage, typed request models, authentication, retries, streaming, and native pagination | Spring Data repositories, mapping, query derivation, and Boot integration |
| [Couch Slacker](https://github.com/Majlanky/couch-slacker) | A Spring Data-style CouchDB library | Repository factory integration, derived Mango queries, declared queries, indexes, and CouchDB integration tests | Current Spring Data APIs, a Spring Data mapping model, native bookmark scrolling, and current Boot packaging |
| [LightCouch](https://github.com/lightcouch/LightCouch) | A lightweight Java client for CouchDB | Compact APIs for documents, views, attachments, changes, replication, and conflicts | Spring Data and Spring Boot integration |
| [CouchRepository](https://github.com/rwitzel/CouchRepository) | CRUD repository adapters over LightCouch and Ektorp | A familiar CRUD interface, view-based queries, custom repository implementations, and driver abstraction | The broader Spring Data repository SPI, derived Mango queries, paging, and Boot integration |

### IBM Cloudant Java SDK provides a strong client-level API

The IBM Cloudant Java SDK focuses on reliable access to Cloudant and supports
Apache CouchDB 3.x for data operations. It covers documents, bulk operations,
attachments, Mango queries and indexes, views, partitions, changes, replication,
and administrative endpoints. It also supplies synchronous and asynchronous
calls, authentication, retry behavior, and bookmark pagination. The project is
actively released; version 0.10.20 was published in July 2026. See its
[README](https://github.com/IBM/cloudant-java-sdk/blob/main/README.md) and
[0.10.20 release](https://github.com/IBM/cloudant-java-sdk/releases/tag/v0.10.20).

Its API follows client and protocol concepts: service calls, endpoint option
objects, and response models. That is an appropriate design for a general SDK,
but it does not provide Spring Data repository factories, persistent entity
mapping, derived query methods, Spring exception translation, or Boot
auto-configuration. CouchWeave can evaluate the SDK as an internal transport
without making its models part of CouchWeave's public repository contract.

### Couch Slacker demonstrates a substantial Spring Data approach

Couch Slacker is the closest project in scope to CouchWeave. It implements
Spring Data repository extension points, parses derived methods with `PartTree`,
translates supported operations to Mango, and offers declared queries, view
queries, and index declarations. These choices provide useful implementation
and test references for a CouchDB store module. Its
[repository factory](https://github.com/Majlanky/couch-slacker/blob/master/src/main/java/com/groocraft/couchdb/slacker/repository/CouchDbRepositoryFactory.java)
shows that the project integrates beyond the `CrudRepository` interface.

The latest published build, 2.4.2, uses Spring Data Commons 2.7.0, Spring
Framework 5.3.24, and Spring Boot 2.7.6, as recorded in its
[`pom.xml`](https://github.com/Majlanky/couch-slacker/blob/v2.4.2/pom.xml).
Current Spring Data versions use changed repository and query extension
contracts, so using this source with a current baseline requires an API port.
The project also uses its own reflection-based entity metadata rather than the
Spring Data mapping model, and its
[documented limitations](https://github.com/Majlanky/couch-slacker#limitations)
identify boundaries around projections, query by example, auditing, attachments,
and several derived operators.

Couch Slacker therefore contributes the strongest prior example of repository
and Mango-query integration, while CouchWeave still needs to define current
mapping, result-processing, paging, conflict, and packaging contracts.

### LightCouch captures CouchDB operations in a compact client

LightCouch offers direct, synchronous APIs for document CRUD, Mango JSON
queries, views, bulk documents, attachments, changes, replication, and conflict
handling. Its small surface makes it a useful reference for the CouchDB
operations a higher-level integration may need to represent.

The project does not depend on Spring or claim Spring Data integration. Its
[build descriptor](https://github.com/lightcouch/LightCouch/blob/master/pom.xml)
targets an earlier Java and dependency baseline, including Java 5 source
compatibility. Bringing it into a current Spring application would require
modernizing the client before adding the separate repository, mapping, query,
and Boot layers.

### CouchRepository deliberately keeps its repository scope narrow

CouchRepository exposes `CrudRepository`-style interfaces over LightCouch and
Ektorp. It supports basic CRUD, custom implementations, and methods backed by
existing CouchDB views. Its README explicitly positions the underlying drivers
as the route for more sophisticated operations and does not plan
`PagingAndSortingRepository` support. See the project's
[scope and usage guidance](https://github.com/rwitzel/CouchRepository/blob/master/README.md).

Its repository factory creates a Java proxy and describes itself as comparable
to Spring Data's `RepositoryFactorySupport`; it does not extend that Spring
infrastructure. Methods that are not CRUD or custom implementations become
view queries rather than property-derived Mango queries. The
[factory](https://github.com/rwitzel/CouchRepository/blob/master/src/main/java/com/github/rwitzel/couchrepository/api/CouchDbCrudRepositoryFactory.java)
and
[query handler](https://github.com/rwitzel/CouchRepository/blob/master/src/main/java/com/github/rwitzel/couchrepository/internal/QueryMethodHandler.java)
make this boundary explicit.

This design succeeds at providing a lightweight repository-shaped adapter. It
also illustrates why API familiarity alone does not supply the mapping,
discovery, query, paging, and lifecycle behavior expected from a current Spring
Data store module.

## CouchDB semantics define the remaining work

The integration gap is not only framework wiring. Some common Spring Data
expectations need CouchDB-specific definitions.

### Revisions need optimistic-locking semantics

CouchDB uses optimistic multi-version concurrency control. An update includes
the current `_rev`, and a stale revision produces a conflict. A Spring Data
module needs to map that revision into entity state and translate a conflict
into a meaningful optimistic-locking exception; serializing `_rev` alone does
not define the complete behavior. The CouchDB documentation describes the
[MVCC model](https://docs.couchdb.org/en/stable/intro/overview.html) and
[application-level conflict handling](https://docs.couchdb.org/en/stable/replication/conflicts.html).

### Native continuation differs from numbered pages

Mango uses opaque bookmarks for efficient continuation. CouchDB documents that
`skip` is not intended for paging, and Mango sorting depends on usable indexes
and consistent sort directions. These constraints mean that some `Pageable`
requests can be supported predictably while others need clear rejection or a
native continuation API. See the CouchDB
[`_find` documentation](https://docs.couchdb.org/en/stable/api/database/find.html)
and Spring Data's
[`ScrollPosition`](https://docs.spring.io/spring-data/commons/reference/api/java/org/springframework/data/domain/ScrollPosition.html)
abstraction.

### Bulk operations report document-level outcomes

CouchDB's
[`_bulk_docs` API](https://docs.couchdb.org/en/stable/api/database/bulk-api.html#db-bulk-docs)
reports the outcome of each document in a bulk write. A repository integration
must preserve partial failures rather than imply that the batch was an atomic
success. The same principle applies across CouchWeave: when CouchDB cannot
represent a Spring Data operation faithfully, the API should expose the
constraint or fail clearly.

## These lessons shape CouchWeave's direction

CouchWeave does not need to reproduce the full surface of a CouchDB client.
Instead, it can keep the transport boundary separate and focus its public API on
the Spring Data contract:

- build repository support on the current Spring Data Commons extension points;
- provide a CouchDB-aware persistent entity and conversion model;
- translate the supported derived-query subset to Mango and document index
  requirements;
- treat revisions, bookmarks, and per-document bulk results as first-class
  semantics;
- separate the framework-independent core from Boot auto-configuration and the
  starter; and
- publish and test an explicit compatibility matrix.

These are planned capabilities, not claims about the current implementation.
CouchWeave remains in its initial design phase.

## The missing layer is worth building

Each project evaluated here solves a real problem: a maintained protocol SDK, a
substantial earlier Spring Data integration, a compact CouchDB client, or a
focused CRUD adapter. CouchWeave benefits from those examples and can avoid
relearning the same integration constraints.

CouchWeave exists because the remaining problem is distinct: connect current
Spring Data conventions to CouchDB without weakening either side of the
contract. The project will be useful only if Spring applications feel familiar
while CouchDB behavior remains explicit, predictable, and testable.
