import "./browserShimsAndPolyfills";

import React from "react";
import ReactDOM from "react-dom";
import { ThemeProvider, Theme } from "@emotion/react";
import { createBrowserHistory } from "history";

import "./app/actions"; //  To initialize parameterized actions
import { makeStore } from "./store";
import AppRoot from "./AppRoot";

import { initializeEnvironment, Environment, basename } from "./environment";
import { Store } from "redux";
import { StateT } from "app-types";
import { TabT } from "./pane/types";

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch

let store: Store<StateT>;
let browserHistory: any;

const initialState = {};

// Render the App including Hot Module Replacement
const renderRoot = (tabs: TabT[], theme: Theme) => {
  browserHistory =
    browserHistory ||
    createBrowserHistory({
      basename: basename(),
    });
  store = store || makeStore(initialState, browserHistory, tabs);

  ReactDOM.render(
    <ThemeProvider theme={theme}>
      <AppRoot store={store} browserHistory={browserHistory} rightTabs={tabs} />
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
