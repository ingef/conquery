import { getType } from "typesafe-actions";

import type { Action } from "../app/actions";

import { clickPaneTab } from "./actions";

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
  action: Action,
): PanesStateT => {
  switch (action.type) {
    case getType(clickPaneTab):
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
