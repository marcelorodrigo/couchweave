# Compatibility Policy

CouchWeave is defining its compatibility contract before it exposes a public
Java API. This document separates versions the project builds against from
versions that CI actively verifies, so an allowed build does not imply an
untested support claim.

## Initial compatibility matrix

| Technology | Supported release line | Current baseline | Verification |
| --- | --- | --- | --- |
| Java | 21 minimum | 21 | CI runs the complete Maven build on Java 21 and 25. Other Java feature releases accepted by the build are not continuously verified unless listed here. |
| Spring Data | 2026.0.x | 2026.0.0 | Managed by the Spring Boot 4.1.0 dependency BOM. |
| Spring Boot | 4.1.x | 4.1.0 | Imported by the parent Maven project and used by the Boot integration modules. |
| Apache CouchDB | 3.5.x primary; 3.4.x compatibility | 3.5 and 3.4 | Repository integration tests will target both lines when CouchDB behavior is implemented. |

The CouchDB entries are compatibility targets, not claims about behavior that
already exists. CouchWeave is still in its design phase and does not yet
provide a working CouchDB integration. The [feature-support policy](supported-features.md)
tracks when planned behavior becomes supported.

## Public artifacts

The initial public Maven coordinates are:

| Coordinate | Responsibility |
| --- | --- |
| `io.github.marcelorodrigo:couchweave-core` | CouchDB mapping, operations, repository implementation, and query support |
| `io.github.marcelorodrigo:couchweave-spring-boot` | Spring Boot auto-configuration |
| `io.github.marcelorodrigo:couchweave-spring-boot-starter` | Opinionated dependency entry point for Spring Boot applications |

These modules currently build as snapshot artifacts but are not published for
application use.

## Pre-1.0 evolution policy

CouchWeave uses semantic versioning, but `0.y` releases remain an API-design
period. Source, binary, and configuration compatibility is not guaranteed
between minor releases such as `0.1` and `0.2`.

- Patch releases within one minor line remain compatible unless a security or
  correctness defect cannot be repaired compatibly.
- When practical, an API deprecated during a minor line remains available for
  the rest of that line and may be removed in the next minor release.
- Breaking changes and removals are identified in release notes with migration
  guidance.
- A feature is not part of the compatibility contract until the
  [support matrix](supported-features.md) marks it as supported.

This policy favors honest support claims while the repository, mapping, and
CouchDB contracts are still being proven. A stricter compatibility and
deprecation policy will accompany the 1.0 release line.
