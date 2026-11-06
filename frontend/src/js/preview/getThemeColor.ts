/**
 * Chart.js needs resolved color strings at runtime, so read the css theme
 * tokens from the root element — downstream `:root` overrides keep working.
 */
export const getCssVarColor = (name: string): string =>
  getComputedStyle(document.documentElement).getPropertyValue(name).trim();
