// Light or dark. Light is the default, whatever the system's setting; the choice is remembered in this browser
// only (localStorage), which is a per-viewer convenience, not application data.

export type Theme = "light" | "dark";

const KEY = "matriz-theme";

export function savedTheme(): Theme {
  try {
    return localStorage.getItem(KEY) === "dark" ? "dark" : "light";
  } catch {
    return "light";   // storage blocked (e.g. a private window): use the default
  }
}

export function applyTheme(theme: Theme) {
  document.documentElement.dataset.theme = theme;
  try {
    localStorage.setItem(KEY, theme);
  } catch {
    // not remembered; the theme still applies to this visit
  }
}
