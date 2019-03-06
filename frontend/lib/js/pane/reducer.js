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
const registerRightPaneTabComponents = components => {
  window.rightPaneTabComponents = components;
};

export const getRightPaneTabComponent = tab => {
  return window.rightPaneTabComponents[tab] || null;
};

export const buildPanesReducer = availableTabs => {
  const tabs = Object.values(availableTabs);

  // Collect reducers
  const tabsReducers = Object.assign(
    {},
    ...tabs.map(tab => ({ [tab.description.key]: tab.reducer }))
  );

  // Collect components
  const tabsComponents = Object.assign(
    {},
    ...tabs.map(tab => ({ [tab.description.key]: tab.component }))
  );
  registerRightPaneTabComponents(tabsComponents);

  const defaultTab = tabs.length
    ? tabs.sort((a, b) => a.description.order - b.description.order)[0]
    : null;

  const initialState: StateType = {
    left: {
      activeTab: "categoryTrees",
      tabs: [
        { label: "leftPane.categoryTrees", key: "categoryTrees", order: 0 },
        { label: "leftPane.previousQueries", key: "previousQueries", order: 1 }
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
