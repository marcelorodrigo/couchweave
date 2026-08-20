---
title: "Documentation"
description: "The current CouchWeave documentation status and the guides planned alongside the first implementation."
---

CouchWeave's core module now provides mapped synchronous operations and
synchronous CRUD repositories as its first public API. This section will grow
with the code as additional contracts are proven.

## What will live here

- supported Java, Spring Data, Spring Boot, and CouchDB versions;
- installation and Spring Boot configuration (planned);
- entity mapping for `_id`, `_rev`, and application fields (available);
- repository CRUD (available) and derived-query support (planned);
- Mango index, sorting, and bookmark requirements (planned);
- optimistic-locking semantics (available) and bulk-operation failure semantics
  (planned); and
- explicit unsupported Spring Data features.

For now, the [design rationale](/why/) explains the gap CouchWeave intends to
fill, and the [roadmap](/roadmap/) tracks the contracts being defined first.
