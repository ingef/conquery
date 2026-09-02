import { type Theme, ThemeProvider } from "@emotion/react";
import { createRoot } from "react-dom/client";
import type { Store } from "redux";

import "../fonts.css";
import "../index.css";

import AppRoot from "./AppRoot";
import type { StateT } from "./app/reducers";
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
      <AppRoot store={store} />
    </ThemeProvider>,
  );
};

export default function conquery({ theme }: { theme: Theme }) {
  return renderRoot(theme);
}
