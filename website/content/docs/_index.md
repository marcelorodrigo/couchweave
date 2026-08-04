---
title: "Documentation"
description: "The current CouchWeave documentation status and the guides planned alongside the first implementation."
---

CouchWeave does not have an implementation or public API to document yet. This
section will grow with the code instead of describing contracts that have not
been proven.

## What will live here

- supported Java, Spring Data, Spring Boot, and CouchDB versions;
- installation and Spring Boot configuration;
- entity mapping for `_id`, `_rev`, and application fields;
- repository CRUD and derived-query support;
- Mango index, sorting, and bookmark requirements;
- optimistic-locking and bulk-operation failure semantics; and
- explicit unsupported Spring Data features.

For now, the [design rationale](/why/) explains the gap CouchWeave intends to
fill, and the [roadmap](/roadmap/) tracks the contracts being defined first.
