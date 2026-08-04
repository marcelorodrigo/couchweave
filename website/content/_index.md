---
title: "CouchWeave"
description: "Spring Data-compatible access to Apache CouchDB without hiding datastore semantics."
hero:
  eyebrow: "Spring Data meets CouchDB"
  title: "Familiar repositories. Native CouchDB behavior."
  summary: "CouchWeave is building a current Spring Data store module for CouchDB—one that respects revisions, Mango queries, bookmarks, indexes, and partial bulk outcomes."
  primary:
    label: "Why CouchWeave exists"
    url: "/why/"
  secondary:
    label: "View the roadmap"
    url: "/roadmap/"
capabilities:
  - title: "Repository-first"
    description: "A Spring Data programming model for CRUD, derived queries, sorting, and datastore-aware continuation."
  - title: "CouchDB-honest"
    description: "Revisions, conflicts, bookmarks, index constraints, and per-document outcomes remain explicit."
  - title: "Built for current Spring"
    description: "Current Spring Data extension points, clean Boot integration, and a published compatibility matrix."
principles:
  - title: "Preserve semantics"
    description: "Do not make CouchDB behave like a relational database when correctness depends on the difference."
  - title: "Fail clearly"
    description: "Reject Spring Data operations that cannot be represented faithfully instead of approximating them silently."
  - title: "Test the real system"
    description: "Verify repository behavior against supported CouchDB versions, not only mocks or protocol fixtures."
modules:
  - name: "couchweave-core"
    description: "Mapping, operations, repositories, and query translation."
  - name: "couchweave-spring-boot"
    description: "Conditional auto-configuration and configuration properties."
  - name: "couchweave-spring-boot-starter"
    description: "The opinionated dependency entry point for Boot applications."
---
