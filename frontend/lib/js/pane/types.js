// @flow

import * as React from "react";

// Used for right pane tabs at the moment
export type TabType = {
  key: string,
  label: string, // Translatable key
  reducer: Function, // combineReducers({ ... }) will be spread on root, see app/reducers.js
  component: React.ComponentType<any> // The tab contents
};
