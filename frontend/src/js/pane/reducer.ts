import { CLICK_PANE_TAB } from "./actionTypes";

export type TabType = {
  label: string;
  key: string;
};

export type StateType = {
  left: {
    activeTab: "conceptTrees" | "previousQueries";
    tabs: TabType[];
  };
  right: {
    activeTab: string;
    tabs: TabType[];
  };
};

export const buildPanesReducer = tabs => {
  const initialState: StateType = {
    left: {
      activeTab: "conceptTrees",
      tabs: [
        { label: "leftPane.conceptTrees", key: "conceptTrees" },
        { label: "leftPane.previousQueries", key: "previousQueries" }
      ]
    },
    right: {
      activeTab: tabs[0].key,
      tabs: tabs.map(tab => ({ label: tab.label, key: tab.key }))
    }
  };

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
        return state;
    }
  };
};
