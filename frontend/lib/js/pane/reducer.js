// @flow

import { combineReducers } from "redux";

import { CLICK_PANE_TAB } from "./actionTypes";

export type TabType = {
  label: string,
  key: string
};

export type StateType = {
  left: {
    activeTab: "categoryTrees" | "previousQueries",
    tabs: TabType[]
  },
  right: {
    activeTab: string,
    tabs: Object
  }
};

// Keep a map of React components for the available tabs outside the Redux state
// Kai's comment: This is really ugly, but at least, this keeps all of the "tabs" magic in this one file
// TODO: Think about some other way
const registerRightPaneTabComponents = components => {
  window.rightPaneTabComponents = components;
};

export const getRightPaneTabComponent = tab => {
  return window.rightPaneTabComponents[tab];
};

export const buildPanesReducer = tabs => {
  // Collect reducers
  const tabsReducers = tabs.reduce((all, tab) => {
    all[tab.description.key] = tab.reducer;

    return all;
  }, {});
  const tabsComponents = tabs.reduce((all, tab) => {
    all[tab.description.key] = tab.component;

    return all;
  }, {});

  registerRightPaneTabComponents(tabsComponents);

  const defaultTab = tabs[0];

  const initialState: StateType = {
    left: {
      activeTab: "categoryTrees",
      tabs: [
        { label: "leftPane.categoryTrees", key: "categoryTrees" },
        { label: "leftPane.previousQueries", key: "previousQueries" }
      ]
    },
    right: {
      activeTab: defaultTab.description.key
    }
  };

  const rightPaneTabsReducer = combineReducers(tabsReducers);

  return (state: StateType = initialState, action: Object): StateType => {
    switch (action.type) {
      case CLICK_PANE_TAB:
        const { paneType, tab } = action.payload;

        return {
          ...state,
          [paneType]: {
            ...state[paneType],
            activeTab: tab
          }
        };
      default:
        return {
          ...state,
          right: {
            ...state.right,
            tabs: rightPaneTabsReducer(state.right.tabs, action)
          }
        };
    }
  };
};
