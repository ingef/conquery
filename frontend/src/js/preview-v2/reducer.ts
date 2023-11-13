import { Table } from "apache-arrow";
import { getType } from "typesafe-actions";

import { Action } from "../app/actions";

import { openPreview, closePreview, loadPreview } from "./actions";

export type PreviewStateT = {
  isOpen: boolean;
  isLoading: boolean;
  dataLoadedForQueryId: number | null;
  tableData: Table | null;
};

const initialState: PreviewStateT = {
  isOpen: false,
  isLoading: false,
  dataLoadedForQueryId: null,
  tableData: null,
};

export default function reducer(
  state: PreviewStateT = initialState,
  action: Action,
): PreviewStateT {
  switch (action.type) {
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
    case getType(loadPreview.request):
      return {
        ...state,
        isLoading: true,
      };
    case getType(loadPreview.failure):
      return {
        ...state,
        isLoading: false,
      };
    case getType(loadPreview.success):
      return {
        ...state,
        isLoading: false,
        dataLoadedForQueryId: action.payload.queryId,
        tableData: action.payload.tableData,
      };
    default:
      return state;
  }
}
