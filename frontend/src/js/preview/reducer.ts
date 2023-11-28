import { getType } from "typesafe-actions";

import type { ColumnDescription } from "../api/types";
import { Action } from "../app/actions";

import { closePreview, loadCSVForPreview, openPreview } from "./actions";

export type PreviewStateT = {
  isOpen: boolean;
  isLoading: boolean;
  dataLoadedForResultUrl: string | null;
  data: {
    csv: string[][] | null;
    resultColumns: ColumnDescription[] | null;
  };
};

const initialState: PreviewStateT = {
  isOpen: false,
  isLoading: false,
  dataLoadedForResultUrl: null,
  data: {
    csv: null,
    resultColumns: null,
  },
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
        isLoading: false,
        dataLoadedForResultUrl: action.payload.resultUrl,
        data: {
          csv: action.payload.csv,
          resultColumns: action.payload.columns,
        },
      };
    case getType(openPreview):
      return {
        ...state,
        isOpen: true,
      };
    case getType(closePreview):
      return {
        ...state,
        isOpen: false,
      };
    default:
      return state;
  }
}
