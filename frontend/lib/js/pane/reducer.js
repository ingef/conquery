// @flow

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

const initialState: StateType = {
  left: {
    activeTab: 'categoryTrees',
    tabs: [
      { label: 'leftPane.categoryTrees', tab: 'categoryTrees' },
      { label: 'leftPane.previousQueries', tab: 'previousQueries' },
    ]
  },
  right: {
    activeTab: 'queryEditor',
    tabs: [
      { label: 'rightPane.queryEditor', tab: 'queryEditor' },
      { label: 'rightPane.timebasedQueryEditor', tab: 'timebasedQueryEditor' },
      { label: 'rightPane.externalForms', tab: 'externalForms' },
    ]
  }
};

const panesReducer = (state: StateType = initialState, action: Object): StateType => {
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
      return state;
  }
};

export default panesReducer;
