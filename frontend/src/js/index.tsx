import { createRoot } from "react-dom/client";
import type { Store } from "redux";

import "../fonts.css";
import "../index.css";

import AppRoot from "./AppRoot";
import type { StateT } from "./app/reducers";
import { type AppTheme, AppThemeContext } from "./app-theme-context";
import { makeStore } from "./store";

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch

let store: Store<StateT>;

const renderRoot = (theme: AppTheme) => {
  store = store || makeStore();

  const root = createRoot(document.getElementById("root")!);

  return root.render(
    <AppThemeContext.Provider value={theme}>
      <AppRoot store={store} />
    </AppThemeContext.Provider>,
  );
};

export default function conquery({ theme }: { theme: AppTheme }) {
  return renderRoot(theme);
}
