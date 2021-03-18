import { CLICK_PANE_TAB } from "./actionTypes";

export type LeftPaneTab = "conceptTrees" | "previousQueries" | "formConfigs";
export interface PanesStateT {
  left: { activeTab: LeftPaneTab };
  right: { activeTab: string | null };
}

const initialState: PanesStateT = {
  left: {
    activeTab: "conceptTrees",
  },
  right: {
    activeTab: null,
  },
};

const reducer = (
  state: PanesStateT = initialState,
  action: any
): PanesStateT => {
  switch (action.type) {
    case CLICK_PANE_TAB:
      const { paneType, tab } = action.payload;

      return {
        ...state,
        [paneType]: {
          ...state[paneType],
          activeTab: tab,
        },
      };
    default:
      return state;
  }
};

export default reducer;
