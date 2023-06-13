import { getType } from "typesafe-actions";

import type {
  ColumnDescription,
  ColumnDescriptionSemanticId,
  EntityInfo,
  HistorySources,
  ResultUrlWithLabel,
  TimeStratifiedInfo,
} from "../api/types";
import type { Action } from "../app/actions";

import {
  closeHistory,
  loadHistoryData,
  loadDefaultHistoryParamsSuccess,
  openHistory,
  resetCurrentEntity,
  resetHistory,
} from "./actions";

// TODO: This is quite inaccurate
export type EntityEvent = {
  dates: {
    from: string; // e.g. 2022-01-31
    to: string; // e.g. 2022-01-31
  };
  [key: string]: any;
};

export interface EntityId {
  id: string;
  kind: ColumnDescriptionSemanticId["kind"];
}

export type EntityHistoryStateT = {
  defaultParams: {
    sources: HistorySources;
    searchConcept: string | null;
    searchFilters: string[];
  };
  isLoading: boolean;
  isOpen: boolean;
  columns: Record<string, ColumnDescription>;
  columnDescriptions: ColumnDescription[];
  resultUrls: ResultUrlWithLabel[];
  label: string;
  entityIds: EntityId[];
  currentEntityUniqueSources: string[];
  currentEntityId: EntityId | null;
  currentEntityData: EntityEvent[];
  currentEntityCsvUrl: string;
  currentEntityInfos: EntityInfo[];
  currentEntityTimeStratifiedInfos: TimeStratifiedInfo[];
};

const initialState: EntityHistoryStateT = {
  defaultParams: {
    sources: { all: [], default: [] },
    searchConcept: null,
    searchFilters: [],
  },
  label: "",
  columns: {},
  columnDescriptions: [],
  resultUrls: [],
  isLoading: false,
  isOpen: false,
  entityIds: [],
  currentEntityUniqueSources: [],
  currentEntityId: null,
  currentEntityData: [],
  currentEntityCsvUrl: "",
  currentEntityInfos: [],
  currentEntityTimeStratifiedInfos: [],
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
          sources: { all: action.payload.all, default: action.payload.default },
          searchConcept: action.payload.searchConcept,
          searchFilters: action.payload.searchFilters || [],
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
    case getType(resetCurrentEntity):
      return {
        ...state,
        currentEntityId: null,
        currentEntityData: [],
        currentEntityCsvUrl: "",
        currentEntityInfos: [],
        currentEntityTimeStratifiedInfos: [],
      };
    case getType(resetHistory):
      return {
        ...state,
        label: "",
        columns: {},
        columnDescriptions: [],
        resultUrls: [],
        entityIds: [],
        currentEntityUniqueSources: [],
        currentEntityId: null,
        currentEntityData: [],
        currentEntityCsvUrl: "",
        currentEntityInfos: [],
        currentEntityTimeStratifiedInfos: [],
      };
    case getType(openHistory):
      return { ...state, isOpen: true };
    case getType(closeHistory):
      return { ...state, isOpen: false };
    default:
      return state;
  }
}
