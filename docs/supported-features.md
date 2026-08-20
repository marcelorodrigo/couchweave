# Feature-Support Policy

A familiar Spring Data interface is useful only when its behavior remains
predictable on CouchDB. This matrix makes the initial release boundary explicit
and avoids presenting planned APIs as implemented capabilities.

## Status definitions

| Status | Meaning |
| --- | --- |
| Supported | Implemented and covered by the automated tests appropriate to the behavior. |
| Planned | Part of the initial release line but not available yet. |
| Deferred | Intentionally outside the initial release line and eligible for later design. |
| Unsupported | Intentionally unavailable because CouchDB cannot provide faithful semantics or CouchWeave rejects the contract. |

The [compatibility policy](compatibility.md) defines the Java, Spring Data,
Spring Boot, and CouchDB versions to which these statuses apply.

## Initial feature matrix

| Area | Capability | Status | Contract |
| --- | --- | --- | --- |
| Build | Maven reactor and module dependency structure | Supported | The parent, three public modules, sample, and coverage report build together on the CI Java matrix. |
| Operations | Synchronous mapped document CRUD | Supported | `CouchWeaveTemplate` supports save, find, existence, and revision-aware deletion without Spring Boot. |
| Mapping | Java fields, document `_id`, and document `_rev` | Supported | `CouchWeaveConverter` maps IDs and revisions, including immutable entities and generated IDs. |
| Repositories | Synchronous document CRUD | Supported | `CouchWeaveRepository` extends Spring Data `CrudRepository`; IDs must be `String`; CRUD delegates to mapped operations while preserving revision semantics. `saveAll` and collection operations are sequential and non-atomic. `findAll` and `count` filter by the exact discriminator, with no pagination or sorting. |
| Repositories | Enabling/configuration | Planned | Public repository activation through `@EnableCouchWeaveRepositories` is planned for a later issue. |
| Concurrency | Revision-based optimistic locking | Supported | CouchDB conflicts surface as `CouchOptimisticLockingFailureException` carrying the entity type, document ID, and attempted revision. Saving an existing document without a revision is rejected before the write; `delete(entity)` enforces a caller snapshot and `deleteById` deletes the current revision without stale detection. No conflict is retried automatically. |
| Queries | Supported derived queries translated to Mango | Planned | Only operators with a documented Mango translation will be accepted. |
| Queries | Index-compatible sorting | Planned | Sorting will require a compatible CouchDB index and predictable direction rules. |
| Pagination | Bookmark-based continuation | Planned | Native Mango bookmarks will represent forward continuation. |
| Bulk writes | Per-document outcomes | Planned | Partial failures will remain visible instead of being reported as an atomic success. |
| Spring Boot | Auto-configuration and starter packaging | Planned | Boot integration will remain separate from the framework-independent core. |
| Access model | Reactive access | Deferred | The initial release line provides synchronous access only. |
| Documents | Attachments | Deferred | Attachment APIs require a separate streaming and metadata contract. |
| Queries | CouchDB views | Deferred | The initial query surface focuses on Mango. |
| Events | Changes feed | Deferred | Continuous and long-poll consumption require a separate lifecycle contract. |
| Operations | Replication | Deferred | Replication administration is outside the initial repository surface. |
| Pagination | Numbered pages and offset-based page navigation | Unsupported | CouchDB bookmarks do not provide stable numbered-page semantics; CouchWeave will not emulate them with `skip`. |
| Consistency | Multi-document transactions | Unsupported | CouchDB does not provide the transactional boundary implied by Spring transaction semantics. |

## Changing a status

A planned capability becomes supported only after its public contract and
failure behavior are documented and its implementation is covered at the
appropriate unit, integration, or real-CouchDB boundary. Moving a deferred or
unsupported capability into the release line requires a focused design issue;
the matrix does not itself commit the project to that work.

The [design rationale](why-couchweave.md) explains why CouchDB-native revisions,
bookmarks, indexes, and per-document outcomes shape these boundaries.
