import { ThemeProvider, Theme } from "@emotion/react";
import { createRoot } from "react-dom/client";
import { Store } from "redux";

import "../fonts.css";

import AppRoot from "./AppRoot";
import GlobalStyles from "./GlobalStyles";
import type { StateT } from "./app/reducers";
import { makeStore } from "./store";

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch

let store: Store<StateT>;

const initialState = {};

const renderRoot = (theme: Theme) => {
  store = store || makeStore(initialState);

  const root = createRoot(document.getElementById("root")!);

  return root.render(
    <ThemeProvider theme={theme}>
      <GlobalStyles />
      <AppRoot store={store} />
    </ThemeProvider>,
  );
};

export default function conquery({ theme }: { theme: Theme }) {
  return renderRoot(theme);
}
