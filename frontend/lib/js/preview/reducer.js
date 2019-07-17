// @flow

import { OPEN_PREVIEW, CLOSE_PREVIEW } from "./actionTypes";

export type StateT = {
  csv: ?(string[][])
};

const initialState: StateT = {
  csv: null
};

export default (state: StateT = initialState, action: Object): StateT => {
  switch (action.type) {
    case CLOSE_PREVIEW:
      return {
        ...state,
        csv: null
      };
    case OPEN_PREVIEW:
      return {
        ...state,
        csv: action.payload.csv
      };
    default:
      return state;
  }
};
