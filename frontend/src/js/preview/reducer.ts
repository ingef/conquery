import type { ColumnDescription } from "../api/types";

import {
  OPEN_PREVIEW,
  CLOSE_PREVIEW,
  LOAD_CSV_START,
  LOAD_CSV_ERROR,
} from "./actionTypes";

export type PreviewStateT = {
  csv: string[][] | null;
  resultColumns: ColumnDescription[] | null;
  isLoading: boolean;
};

const initialState: PreviewStateT = {
  csv: null,
  resultColumns: null,
  isLoading: false,
};

export default (
  state: PreviewStateT = initialState,
  action: any,
): PreviewStateT => {
  switch (action.type) {
    case LOAD_CSV_START:
      return {
        ...state,
        isLoading: true,
      };
    case LOAD_CSV_ERROR:
      return {
        ...state,
        isLoading: false,
      };
    case CLOSE_PREVIEW:
      return {
        ...state,
        csv: null,
        resultColumns: null,
      };
    case OPEN_PREVIEW:
      return {
        ...state,
        csv: action.payload.csv,
        resultColumns: action.payload.columns,
        isLoading: false,
      };
    default:
      return state;
  }
};
