import "./browserShimsAndPolyfills";

import React from "react";
import ReactDOM from "react-dom";
import { ThemeProvider } from "emotion-theming";
import { createBrowserHistory } from "history";

import "./app/actions"; //  To initialize parameterized actions
import { makeStore } from "./store";
import AppRoot from "./AppRoot";

import { initializeEnvironment, Environment, basename } from "./environment";

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch

let store;
let browserHistory;
const initialState = {};

// Render the App including Hot Module Replacement
const renderRoot = (tabs: Object, theme) => {
  browserHistory =
    browserHistory ||
    createBrowserHistory({
      basename: basename()
    });
  store = store || makeStore(initialState, browserHistory, tabs);

  ReactDOM.render(
    <ThemeProvider theme={theme}>
      <AppRoot store={store} browserHistory={browserHistory} rightTabs={tabs} />
    </ThemeProvider>,
    document.getElementById("root")
  );
};

export default function conquery(
  environment: Environment,
  tabs: Object,
  theme: Object // React-Emotion theme, will at some point completely replace sass
) {
  initializeEnvironment(environment);
  renderRoot(tabs, theme);
}
