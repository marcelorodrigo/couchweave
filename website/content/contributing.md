---
title: "Contributing"
description: "How to follow CouchWeave development and contribute design feedback while the project is taking shape."
---

CouchWeave is still establishing its public contracts. Contributions are most
useful when they expose a concrete Spring Data expectation, CouchDB constraint,
or compatibility risk that the design needs to address.

## Start with the context

Read [Why CouchWeave Exists](/why/) and the [current roadmap](/roadmap/). They
explain the integration gap and the boundaries the project intends to preserve.

## Raise focused proposals

Use the [GitHub issue tracker](https://github.com/marcelorodrigo/couchweave/issues)
for design questions, capability requests, and implementation proposals. Include:

- the Spring Data contract or CouchDB behavior involved;
- the expected application-facing behavior;
- failure cases or compatibility constraints; and
- references to relevant framework or CouchDB documentation.

A full development setup and pull-request guide will be published with the
initial Java build. Until then, focused design discussions are more valuable
than speculative implementation patches.
