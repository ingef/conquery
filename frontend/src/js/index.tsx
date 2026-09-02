import { type Theme, ThemeProvider } from "@emotion/react";
import { createRoot } from "react-dom/client";
import type { Store } from "redux";

import "../fonts.css";
import "../index.css";

import AppRoot from "./AppRoot";
import type { StateT } from "./app/reducers";
import { AppThemeContext } from "./app-theme-context";
import { makeStore } from "./store";

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch

let store: Store<StateT>;

const renderRoot = (theme: Theme) => {
  store = store || makeStore();

  const root = createRoot(document.getElementById("root")!);

  return root.render(
    <ThemeProvider theme={theme}>
      <AppThemeContext.Provider
        value={{ img: theme.img, palette: theme.col.palette }}
      >
        <AppRoot store={store} />
      </AppThemeContext.Provider>
    </ThemeProvider>,
  );
};

export default function conquery({ theme }: { theme: Theme }) {
  return renderRoot(theme);
}
