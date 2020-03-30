import {
  OPEN_PREVIEW,
  CLOSE_PREVIEW,
  LOAD_CSV_START,
  LOAD_CSV_ERROR
} from "./actionTypes";

export type StateT = {
  csv: string[][] | null;
  isLoading: boolean;
};

const initialState: StateT = {
  csv: null,
  isLoading: false
};

export default (state: StateT = initialState, action: Object): StateT => {
  switch (action.type) {
    case LOAD_CSV_START:
      return {
        ...state,
        isLoading: true
      };
    case LOAD_CSV_ERROR:
      return {
        ...state,
        isLoading: false
      };
    case CLOSE_PREVIEW:
      return {
        ...state,
        csv: null
      };
    case OPEN_PREVIEW:
      return {
        ...state,
        csv: action.payload.csv,
        isLoading: false
      };
    default:
      return state;
  }
};
