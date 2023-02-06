import { ThemeProvider, Theme } from "@emotion/react";
import { createRoot } from "react-dom/client";
import { Store } from "redux";

import "../fonts.css";

import AppRoot from "./AppRoot";
import GlobalStyles from "./GlobalStyles";
import type { StateT } from "./app/reducers";
import { initializeEnvironment, CustomEnvironment } from "./environment";
import { TabT } from "./pane/types";
import { makeStore } from "./store";

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch

let store: Store<StateT>;

const initialState = {};

// Render the App including Hot Module Replacement
const renderRoot = (tabs: TabT[], theme: Theme) => {
  store = store || makeStore(initialState);

  const root = createRoot(document.getElementById("root")!);

  return root.render(
    <ThemeProvider theme={theme}>
      <GlobalStyles />
      <AppRoot store={store} rightTabs={tabs} />
    </ThemeProvider>,
  );
};

export default function conquery({
  theme,
  tabs,
  customEnvironment,
}: {
  theme: Theme; // React-Emotion theme, will at some point completely replace sass
  tabs: TabT[];
  customEnvironment: CustomEnvironment;
}) {
  initializeEnvironment(customEnvironment);
  renderRoot(tabs, theme);
}
