import "./browserShimsAndPolyfills";

import React from "react";
import ReactDOM from "react-dom";
import { ThemeProvider, Theme } from "@emotion/react";

import { makeStore } from "./store";
import AppRoot from "./AppRoot";

import { initializeEnvironment, Environment } from "./environment";
import { Store } from "redux";
import { StateT } from "app-types";
import { TabT } from "./pane/types";

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
    document.getElementById("root")
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
