import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const expectedPages = [
  "index.html",
  "why/index.html",
  "roadmap/index.html",
  "docs/index.html",
  "contributing/index.html",
  "legal/index.html",
  "404.html",
];

test("production build contains every public entry point", () => {
  for (const page of expectedPages) {
    const html = readFileSync(new URL(`../public/${page}`, import.meta.url), "utf8");
    assert.match(html, /<title>[^<]+<\/title>/, `${page} has a title`);
    assert.match(html, /<meta name=description content="[^"]+">/, `${page} has a description`);
  }
});

test("home page uses production canonicals and fingerprinted assets", () => {
  const html = readFileSync(new URL("../public/index.html", import.meta.url), "utf8");
  assert.match(html, /rel=canonical href=https:\/\/couchweave\.marcelorodrigo\.com\//);
  assert.match(html, /href=\/css\/main\.[a-f0-9]+\.css/);
  assert.match(html, /src=\/js\/theme\.[a-f0-9]+\.js/);
  assert.doesNotMatch(html, /google-analytics|plausible|cloudflareinsights/i);
});

test("generated infrastructure files target the production site", () => {
  const robots = readFileSync(new URL("../public/robots.txt", import.meta.url), "utf8");
  const sitemap = readFileSync(new URL("../public/sitemap.xml", import.meta.url), "utf8");
  const headers = readFileSync(new URL("../public/_headers", import.meta.url), "utf8");
  assert.match(robots, /https:\/\/couchweave\.marcelorodrigo\.com\/sitemap\.xml/);
  assert.match(sitemap, /https:\/\/couchweave\.marcelorodrigo\.com\/why\//);
  assert.match(headers, /Content-Security-Policy:/);
  assert.match(headers, /X-Robots-Tag: noindex/);
});

test("social preview image has the required Open Graph dimensions", () => {
  const image = readFileSync(new URL("../public/brand/social-card.png", import.meta.url));
  assert.equal(image.toString("ascii", 1, 4), "PNG");
  assert.equal(image.readUInt32BE(16), 1200);
  assert.equal(image.readUInt32BE(20), 630);
});
