import { getType } from "typesafe-actions";

import type { ColumnDescription } from "../api/types";
import { Action } from "../app/actions";

import { closePreview, loadCSVForPreview } from "./actions";

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

export default function reducer(
  state: PreviewStateT = initialState,
  action: Action,
): PreviewStateT {
  switch (action.type) {
    case getType(loadCSVForPreview.request):
      return {
        ...state,
        isLoading: true,
      };
    case getType(loadCSVForPreview.failure):
      return {
        ...state,
        isLoading: false,
      };
    case getType(loadCSVForPreview.success):
      return {
        ...state,
        csv: action.payload.csv,
        resultColumns: action.payload.columns,
        isLoading: false,
      };
    case getType(closePreview):
      return {
        ...state,
        csv: null,
        resultColumns: null,
      };
    default:
      return state;
  }
}
