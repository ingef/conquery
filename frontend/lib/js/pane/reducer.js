// @flow

import { combineReducers }              from 'redux';

import { CLICK_PANE_TAB }  from './actionTypes';

export type TabType = {
  label: string,
  tab: string
};

export type StateType = {
  left: {
    activeTab: 'categoryTrees' | 'previousQueries',
    tabs: TabType[]
  },
  right: {
    activeTab: 'queryEditor' | 'timebasedQueryEditor' | 'externalForms',
    tabs: TabType[]
  }
};

export const buildPanesReducer = (availableTabs) => {
  const tabs = Object.values(availableTabs);

  // collect reducers from form extension
  const tabsReducers =
    Object.assign({}, ...tabs.map(tab => ({[tab.key]: tab.reducer})));

  const defaultTab = tabs.length ? tabs.sort((a, b) => a.order - b.order)[0] : null;

  const initialState: StateType = {
    left: {
      activeTab: 'categoryTrees',
      tabs: [
        'categoryTrees': { label: 'leftPane.categoryTrees', key: 'categoryTrees', order: 0 },
        'previousQueries': { label: 'leftPane.previousQueries', key: 'previousQueries', order: 1 },
      ]
    },
    right: {
      activeTab: defaultTab.key,
      // tabs: [
      //   { label: 'rightPane.queryEditor', tab: 'queryEditor' },
      //   { label: 'rightPane.timebasedQueryEditor', tab: 'timebasedQueryEditor' },
      //   { label: 'rightPane.externalForms', tab: 'externalForms' },
      // ]
    }
  };

  const rightPaneTabsReducer = combineReducers({
    // availableTabs: (state = availableTabs) => state,
    ...tabsReducers
  });

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
