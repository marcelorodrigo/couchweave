# CouchWeave website

This directory contains the complete source for the CouchWeave project website.
It is intentionally independent of the future Java build.

## Local development

Install Hugo Extended 0.164.0, Node 24.14.0, and pnpm 11.10.0, then run:

```shell
pnpm install --frozen-lockfile
pnpm dev
```

Open <http://localhost:1313>. Before committing a website change, run:

```shell
pnpm check
```

## Cloudflare Pages

Connect `marcelorodrigo/couchweave` using these settings:

| Setting | Value |
| --- | --- |
| Production branch | `master` |
| Root directory | `website` |
| Build command | `pnpm run build:cloudflare` |
| Build output directory | `public` |
| Build watch include | `website/*` |

Set `HUGO_VERSION=0.164.0`, `NODE_VERSION=24.14.0`, and
`PNPM_VERSION=11.10.0` for production and previews. Set
`HUGO_BASEURL=https://couchweave.marcelorodrigo.com/` for production only;
preview builds use Cloudflare's `CF_PAGES_URL` automatically.
