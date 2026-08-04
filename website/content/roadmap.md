---
title: "Roadmap"
description: "The current CouchWeave project status and the sequence planned for its first implementation milestones."
---

CouchWeave is in its design phase. There are no published artifacts or working
repository integrations yet. The immediate work is to make the contracts
explicit before implementation choices make them expensive to change.

## Now — define the foundation

- Select and isolate the CouchDB transport strategy.
- Define the Java package namespace and Maven coordinates.
- Establish supported Java, Spring Data, Spring Boot, and CouchDB versions.
- Specify mapping rules for document identifiers and revisions.
- Define save, conflict, bulk-operation, and exception contracts.

## Next — prove the repository model

- Implement persistent entity metadata and conversion.
- Build repository discovery, factory support, and CRUD operations.
- Translate a documented subset of derived queries to Mango selectors.
- Test revision conflicts and repository behavior against real CouchDB.

## Later — widen the supported surface

- Add bookmark-based continuation and predictable sorting.
- Publish index requirements and unsupported query operations.
- Add Spring Boot auto-configuration and the starter module.
- Publish reference documentation and a compatibility matrix with each release.

The roadmap is directional, not a release promise. Each milestone should leave
CouchDB behavior visible and the supported Spring Data surface unambiguous.
