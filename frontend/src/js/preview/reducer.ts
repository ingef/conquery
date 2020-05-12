import {
  OPEN_PREVIEW,
  CLOSE_PREVIEW,
  LOAD_CSV_START,
  LOAD_CSV_ERROR
} from "./actionTypes";

export type PreviewStateT = {
  csv: string[][] | null;
  isLoading: boolean;
};

const initialState: PreviewStateT = {
  csv: null,
  isLoading: false
};

export default (
  state: PreviewStateT = initialState,
  action: Object
): PreviewStateT => {
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
