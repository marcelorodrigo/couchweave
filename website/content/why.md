---
title: "Why CouchWeave Exists"
description: "The gap between Java CouchDB clients and a current, datastore-aware Spring Data integration."
---

Java developers can already connect to CouchDB with capable clients. The gap
appears one layer higher: Spring applications still lack a current store module
that combines familiar repository conventions with CouchDB's revision, query,
indexing, and pagination semantics. Without that layer, teams either work at the
client API or accept an abstraction that covers only part of the Spring Data
programming model.

CouchWeave targets that missing layer. It is for Spring developers who want
repository-style data access without pretending CouchDB is a relational
database or hiding behavior that affects correctness.

> This assessment reflects public documentation and source code available on
> 4 August 2026. The projects discussed here continue to evolve. CouchWeave is
> independent of them and recognizes their contributions to the Java and
> CouchDB ecosystems.

## A repository interface is only the visible edge

A Spring Data store module connects datastore behavior to several framework
contracts. Implementing `CrudRepository` provides a recognizable API, but it
does not define repository discovery, mapping, query lookup, result processing,
or exception translation.

| Area | Integration CouchWeave needs |
| --- | --- |
| Repositories | Discovery, factory support, fragments, query lookup, and multi-store identification |
| Mapping | Persistent entity metadata, IDs, revisions, conversion, object creation, and callbacks |
| Queries | Derived method parsing, Mango translation, result processing, and explicit unsupported operations |
| CouchDB semantics | Revision conflicts, partial bulk outcomes, index constraints, and bookmark continuation |
| Spring integration | Consistent operation contracts and exception translation |
| Spring Boot | Conditional auto-configuration, typed properties, and starter packaging |
| Compatibility | A tested Java, Spring Data, Spring Boot, and CouchDB version matrix |

Spring Data exposes extension points such as
[`RepositoryFactorySupport`](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/repository/core/support/RepositoryFactorySupport.html)
and
[`MappingContext`](https://docs.spring.io/spring-data/commons/reference/api/java/org/springframework/data/mapping/context/MappingContext.html)
for these responsibilities. CouchWeave needs to connect them to CouchDB behavior
as one coherent contract.

## Existing projects cover valuable parts of the journey

These projects solve different problems and come from different generations of
the Java ecosystem. Their boundaries clarify the work that remains; they do not
form a ranking.

| Project | Strength | Boundary relevant to CouchWeave |
| --- | --- | --- |
| [IBM Cloudant Java SDK](https://github.com/IBM/cloudant-java-sdk) | Broad, maintained client-level protocol coverage | No Spring Data repository, mapping, query derivation, or Boot integration |
| [Couch Slacker](https://github.com/Majlanky/couch-slacker) | Repository factories, derived Mango queries, declared queries, and indexes | Built on an earlier Spring generation with its own entity metadata and paging boundaries |
| [LightCouch](https://github.com/lightcouch/LightCouch) | Compact APIs for documents, views, changes, replication, and conflicts | No Spring Data or Spring Boot integration |
| [CouchRepository](https://github.com/rwitzel/CouchRepository) | Focused CRUD adapters and view-backed queries | Does not implement the broader Spring Data repository SPI or derived Mango queries |

### A capable transport does not define a repository model

The IBM Cloudant Java SDK covers documents, bulk operations, attachments, Mango
queries and indexes, views, partitions, changes, replication, authentication,
retries, and bookmark pagination. That is the right surface for a general SDK.
It does not decide how a Spring persistent entity carries `_rev`, how derived
methods become selectors, or how a conflict becomes a Spring data-access error.

CouchWeave can evaluate such a client behind a transport boundary without
making client-specific models part of its public repository contract.

### Prior Spring Data work proves both the value and the migration cost

Couch Slacker comes closest to CouchWeave's intended scope. It integrates with
repository factories, parses methods with `PartTree`, and translates supported
operations to Mango. Its source offers a strong reference for repository and
index behavior.

Its published baseline uses Spring Data Commons 2.7 and Spring Boot 2.7. Current
Spring Data releases have changed extension contracts, so adopting that work is
an API port rather than a dependency update. CouchWeave also needs current
mapping, result-processing, scrolling, conflict, and packaging definitions.

### Lightweight adapters show where familiarity stops

LightCouch provides a compact direct client. CouchRepository places a familiar
CRUD shape over LightCouch and Ektorp, while routing more advanced work to the
underlying drivers. Both approaches are useful within their chosen boundaries.

They also show why an interface that resembles Spring Data is not the same as a
current Spring Data store module. Discovery, mapping, derived queries, lifecycle
behavior, paging, and failure translation remain separate work.

## CouchDB semantics determine the hard contracts

The framework wiring is not the only design challenge. Several common Spring
Data expectations need CouchDB-specific answers.

### Revisions are optimistic-locking state

CouchDB updates include the current `_rev`; stale revisions produce conflicts.
Mapping `_rev` onto an entity is necessary but incomplete. A repository save
must propagate the new revision and translate stale updates into a meaningful
optimistic-locking failure. CouchDB's documentation explains its
[MVCC model](https://docs.couchdb.org/en/stable/intro/overview.html) and
[application-level conflict handling](https://docs.couchdb.org/en/stable/replication/conflicts.html).

### Bookmarks are not numbered pages

Mango uses opaque bookmarks for efficient continuation. CouchDB does not intend
`skip` as its paging mechanism, and Mango sorting depends on usable indexes with
consistent directions. Some `Pageable` requests may have predictable mappings;
others need rejection or a native continuation API. The relevant contracts are
CouchDB's [`_find` API](https://docs.couchdb.org/en/stable/api/database/find.html)
and Spring Data's
[`ScrollPosition`](https://docs.spring.io/spring-data/commons/reference/api/java/org/springframework/data/domain/ScrollPosition.html).

### Bulk writes are not atomic batches

CouchDB's
[`_bulk_docs` API](https://docs.couchdb.org/en/stable/api/database/bulk-api.html#db-bulk-docs)
reports an outcome for every document. A repository integration must preserve
partial failures instead of presenting the batch as one atomic success.

The same rule applies across the project: when CouchDB cannot represent a
Spring Data operation faithfully, CouchWeave should expose the constraint or
fail with a specific explanation.

## The design direction

CouchWeave will keep the transport boundary separate and focus its public API
on a current Spring Data contract:

- build repository support on current Spring Data Commons extension points;
- provide CouchDB-aware persistent entity metadata and conversion;
- translate a documented derived-query subset to Mango;
- treat revisions, bookmarks, indexes, and partial bulk results as first-class;
- separate the core integration from Boot auto-configuration and the starter;
- publish an explicit, tested compatibility matrix.

These are planned capabilities, not claims about a working implementation.

## The missing layer is worth treating as its own system

Each existing project contributes something CouchWeave can learn from: a
maintained protocol SDK, an earlier repository integration, a compact direct
client, or a focused CRUD adapter. None should be stretched beyond its intended
scope to make the comparison work.

CouchWeave is worthwhile only if Spring applications feel familiar while
CouchDB behavior remains explicit, predictable, and testable. That tension is
not an inconvenience to hide; it is the central design problem.
