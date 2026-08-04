import assert from "node:assert/strict";
import test from "node:test";

import {
  STORAGE_KEY,
  createThemeController,
  normalizeTheme,
  readStoredTheme,
  resolveTheme,
} from "../assets/js/theme.js";

function createToggle() {
  const icons = [
    { dataset: { themeIcon: "sun" }, hidden: false },
    { dataset: { themeIcon: "moon" }, hidden: true },
  ];
  for (const icon of icons) {
    icon.classList = {
      toggle: (_name, hidden) => { icon.hidden = hidden; },
    };
  }
  return {
    label: "",
    icons,
    setAttribute(name, value) {
      if (name === "aria-label") this.label = value;
    },
    querySelectorAll() {
      return icons;
    },
  };
}

test("normalizeTheme accepts supported values only", () => {
  assert.equal(normalizeTheme("light"), "light");
  assert.equal(normalizeTheme("dark"), "dark");
  assert.equal(normalizeTheme("sepia"), null);
});

test("resolveTheme uses the system preference without a stored override", () => {
  assert.equal(resolveTheme(null, true), "dark");
  assert.equal(resolveTheme(null, false), "light");
});

test("resolveTheme gives a stored preference priority", () => {
  assert.equal(resolveTheme("light", true), "light");
  assert.equal(resolveTheme("dark", false), "dark");
});

test("readStoredTheme handles blocked storage", () => {
  const storage = { getItem: () => { throw new Error("blocked"); } };
  assert.equal(readStoredTheme(storage), null);
});

test("controller applies and persists the opposite theme", () => {
  const root = { dataset: {} };
  const values = new Map([[STORAGE_KEY, "dark"]]);
  const storage = {
    getItem: (key) => values.get(key),
    setItem: (key, value) => values.set(key, value),
  };
  const toggle = createToggle();
  const controller = createThemeController({
    root,
    storage,
    mediaQuery: { matches: false },
    findToggles: () => [toggle],
  });

  assert.equal(controller.apply(), "dark");
  assert.equal(toggle.label, "Switch to light theme");
  assert.equal(toggle.icons[0].hidden, false);
  assert.equal(toggle.icons[1].hidden, true);
  assert.equal(controller.toggle(), "light");
  assert.equal(root.dataset.theme, "light");
  assert.equal(values.get(STORAGE_KEY), "light");
  assert.equal(toggle.icons[0].hidden, true);
  assert.equal(toggle.icons[1].hidden, false);
});

test("controller follows system changes only without an override", () => {
  const root = { dataset: {} };
  const mediaQuery = { matches: false };
  const controller = createThemeController({
    root,
    storage: { getItem: () => null },
    mediaQuery,
    findToggles: () => [createToggle()],
  });

  controller.apply();
  assert.equal(root.dataset.theme, "light");
  mediaQuery.matches = true;
  controller.handleSystemChange();
  assert.equal(root.dataset.theme, "dark");
});
