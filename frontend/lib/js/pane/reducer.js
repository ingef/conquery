// @flow

import T                   from 'i18n-react';
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
    activeTab: 'queryEditor' | 'timebasedQueryEditor' | 'form',
    tabs: TabType[]
  }
};

const initialState: StateType = {
  left: {
    activeTab: 'categoryTrees',
    tabs: [
      { label: T.translate('leftPane.categoryTrees'), tab: 'categoryTrees' },
      { label: T.translate('leftPane.previousQueries'), tab: 'previousQueries' },
    ]
  },
  right: {
    activeTab: 'queryEditor',
    tabs: [
      { label: T.translate('rightPane.queryEditor'), tab: 'queryEditor' },
      { label: T.translate('rightPane.timebasedQueryEditor'), tab: 'timebasedQueryEditor' },
      { label: T.translate('rightPane.form'), tab: 'form' },
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
