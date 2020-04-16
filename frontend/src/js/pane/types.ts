import * as React from "react";

export type SupportedTabsT = "timebasedQueryEditor" | "queryEditor";

// Used for right pane tabs at the moment
export type TabT = {
  key: string;
  label: string; // Translatable key
  reducer: Function; // combineReducers({ ... }) will be spread on root, see app/reducers.js
  component: React.ComponentType<any>; // The tab contents
};
