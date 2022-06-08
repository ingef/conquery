import { getType } from "typesafe-actions";

import type { TableT } from "../api/types";
import type { Action } from "../app/actions";

import {
  closeHistory,
  initHistoryData,
  loadDefaultHistoryParamsSuccess,
  openHistory,
} from "./actions";

export type EntityHistoryStateT = {
  defaultParams: {
    sources: TableT["id"][];
  };
  isLoading: boolean;
  isOpen: boolean;
  entityIds: string[];
  currentEntityId: string | null;
  currentEntityData: string[][];
};

const initialState: EntityHistoryStateT = {
  defaultParams: {
    sources: [],
  },
  isLoading: false,
  isOpen: false,
  entityIds: [],
  currentEntityId: null,
  currentEntityData: [],
};

export default function reducer(
  state: EntityHistoryStateT = initialState,
  action: Action,
): EntityHistoryStateT {
  switch (action.type) {
    case getType(loadDefaultHistoryParamsSuccess):
      return {
        ...state,
        defaultParams: {
          sources: action.payload.sources,
        },
      };
    case getType(initHistoryData.request):
      return { ...state, isLoading: true };
    case getType(initHistoryData.failure):
      return { ...state, isLoading: false };
    case getType(initHistoryData.success):
      return {
        ...state,
        ...action.payload,
        isLoading: false,
      };
    case getType(openHistory):
      return { ...state, isOpen: true };
    case getType(closeHistory):
      return { ...state, isOpen: false };
    default:
      return state;
  }
}
