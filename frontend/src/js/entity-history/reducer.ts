import { getType } from "typesafe-actions";

import type { TableT } from "../api/types";
import type { Action } from "../app/actions";

import {
  closeHistory,
  loadHistoryData,
  loadDefaultHistoryParamsSuccess,
  openHistory,
} from "./actions";

// TODO: This is quite inaccurate
export type EntityEvent = { [key: string]: any };

export type EntityHistoryStateT = {
  defaultParams: {
    sources: TableT["id"][];
  };
  isLoading: boolean;
  isOpen: boolean;
  entityIds: string[];
  currentEntityId: string | null;
  currentEntityData: EntityEvent[];
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
    case getType(loadHistoryData.request):
      return { ...state, isLoading: true };
    case getType(loadHistoryData.failure):
      return { ...state, isLoading: false };
    case getType(loadHistoryData.success):
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
