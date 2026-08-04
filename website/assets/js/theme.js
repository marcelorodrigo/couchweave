export const STORAGE_KEY = "couchweave-theme";
const THEMES = new Set(["light", "dark"]);

export function normalizeTheme(value) {
  return THEMES.has(value) ? value : null;
}

export function resolveTheme(storedTheme, prefersDark) {
  return normalizeTheme(storedTheme) ?? (prefersDark ? "dark" : "light");
}

export function readStoredTheme(storage) {
  try {
    return normalizeTheme(storage?.getItem(STORAGE_KEY));
  } catch {
    return null;
  }
}

export function writeStoredTheme(storage, theme) {
  try {
    storage?.setItem(STORAGE_KEY, theme);
  } catch {
    // The selected theme still applies for the current page.
  }
}

export function createThemeController({ root, storage, mediaQuery, findToggles }) {
  let storedTheme = readStoredTheme(storage);

  const apply = () => {
    const theme = resolveTheme(storedTheme, mediaQuery.matches);
    root.dataset.theme = theme;
    for (const toggle of findToggles()) {
      const nextTheme = theme === "dark" ? "light" : "dark";
      const nextThemeIcon = nextTheme === "dark" ? "moon" : "sun";
      toggle.setAttribute("aria-label", `Switch to ${nextTheme} theme`);
      for (const icon of toggle.querySelectorAll("[data-theme-icon]")) {
        icon.classList.toggle("hidden", icon.dataset.themeIcon !== nextThemeIcon);
      }
    }
    return theme;
  };

  const toggle = () => {
    const currentTheme = resolveTheme(storedTheme, mediaQuery.matches);
    storedTheme = currentTheme === "dark" ? "light" : "dark";
    writeStoredTheme(storage, storedTheme);
    return apply();
  };

  const handleSystemChange = () => {
    if (storedTheme === null) {
      apply();
    }
  };

  return {
    apply,
    toggle,
    handleSystemChange,
  };
}

function startThemeController() {
  const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
  const findToggles = () => document.querySelectorAll("[data-theme-toggle]");
  const controller = createThemeController({
    root: document.documentElement,
    storage: window.localStorage,
    mediaQuery,
    findToggles,
  });

  controller.apply();
  mediaQuery.addEventListener("change", controller.handleSystemChange);

  const bindToggles = () => {
    controller.apply();
    for (const toggle of findToggles()) {
      toggle.addEventListener("click", controller.toggle);
    }
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", bindToggles, { once: true });
  } else {
    bindToggles();
  }
}

if (typeof window !== "undefined" && typeof document !== "undefined") {
  startThemeController();
}
