import { ThemeProvider, Theme } from "@emotion/react";
import { StateT } from "app-types";
import React from "react";
import ReactDOM from "react-dom";
import { Store } from "redux";

import AppRoot from "./AppRoot";
import "./browserShimsAndPolyfills";
import { initializeEnvironment, Environment } from "./environment";
import { TabT } from "./pane/types";
import { makeStore } from "./store";

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch

let store: Store<StateT>;

const initialState = {};

// Render the App including Hot Module Replacement
const renderRoot = (tabs: TabT[], theme: Theme) => {
  store = store || makeStore(initialState, tabs);

  ReactDOM.render(
    <ThemeProvider theme={theme}>
      <AppRoot store={store} rightTabs={tabs} />
    </ThemeProvider>,
    document.getElementById("root"),
  );
};

export default function conquery({
  environment,
  tabs,
  theme,
}: {
  environment: Environment;
  tabs: TabT[];
  theme: Theme; // React-Emotion theme, will at some point completely replace sass
}) {
  initializeEnvironment(environment);
  renderRoot(tabs, theme);
}
