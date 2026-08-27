import { getType } from "typesafe-actions";

import type {
  ColumnDescription,
  DatasetT,
  GetQueryResponseDoneT,
  ResultUrlWithLabel,
} from "../api/types";
import type { Action } from "../app/actions";

import {
  type QueryTypeT,
  queryResultErrorAction,
  queryResultReset,
  queryResultRunning,
  queryResultStart,
  queryResultSuccess,
  startQuery,
  stopQuery,
} from "./actions";

interface APICallType {
  loading?: boolean;
  success?: boolean;
  error?: string | boolean | null;
  errorContext?: Record<string, string>;
}

interface QueryResultT extends APICallType {
  datasetId?: string;
  resultLabel?: string;
  resultCount?: number | null;
  resultUrls?: ResultUrlWithLabel[];
  resultColumns?: ColumnDescription[] | null;
  queryType?: "CONCEPT_QUERY" | "SECONDARY_ID_QUERY";
  previewAvailable?: boolean;
}

export interface QueryRunnerStateT {
  runningQuery: string | null;
  progress?: number;
  queryRunning: boolean;
  startQuery: APICallType;
  stopQuery: APICallType;
  queryResult: QueryResultT | null;
}

const getQueryResult = (
  data: GetQueryResponseDoneT,
  datasetId: DatasetT["id"],
) => {
  return {
    datasetId,
    loading: false,
    success: true,
    error: null,
    resultLabel: data.label,
    resultCount: data.numberOfResults,
    resultUrls: data.resultUrls,
    resultColumns: data.columnDescriptions,
    queryType: data.queryType,
    previewAvailable: data.previewAvailable,
  };
};

export default function createQueryRunnerReducer(type: QueryTypeT) {
  const initialState: QueryRunnerStateT = {
    runningQuery: null,
    queryRunning: false,
    startQuery: {},
    stopQuery: {},
    queryResult: null,
  };

  const isForThisQueryType = (action: Action) =>
    (action as { payload?: { queryType?: QueryTypeT } }).payload?.queryType ===
    type;

  return (
    state: QueryRunnerStateT = initialState,
    action: Action,
  ): QueryRunnerStateT => {
    // Every action handled below carries the query type it belongs to
    if (!isForThisQueryType(action)) return state;

    switch (action.type) {
      case getType(startQuery.request):
        return {
          ...state,
          stopQuery: {},
          startQuery: { loading: true },
          queryResult: null,
        };
      case getType(startQuery.success):
        return {
          ...state,
          runningQuery: action.payload.data.id,
          queryRunning: true,
          stopQuery: {},
          startQuery: { success: true },
        };
      case getType(startQuery.failure):
        return {
          ...state,
          stopQuery: {},
          startQuery: {
            error: action.payload.message || action.payload.status,
          },
        };
      // To cancel a query
      case getType(stopQuery.request):
        return { ...state, startQuery: {}, stopQuery: { loading: true } };
      case getType(stopQuery.success):
        return {
          ...state,
          runningQuery: null,
          progress: undefined,
          queryRunning: false,
          startQuery: {},
          stopQuery: { success: true },
        };
      case getType(stopQuery.failure):
        return {
          ...state,
          startQuery: {},
          stopQuery: {
            error: action.payload.message || action.payload.status,
          },
        };

      // To check for query results
      case getType(queryResultStart):
        return { ...state, queryResult: { loading: true } };
      case getType(queryResultRunning):
        return { ...state, progress: action.payload.progress };
      case getType(queryResultReset):
        return { ...state, queryResult: { loading: false } };
      case getType(queryResultSuccess):
        return {
          ...state,
          queryResult: getQueryResult(
            action.payload.data,
            action.payload.datasetId,
          ),
          runningQuery: null,
          progress: undefined,
          queryRunning: false,
        };
      case getType(queryResultErrorAction):
        return {
          ...state,
          runningQuery: null,
          progress: undefined,
          queryRunning: false,
          queryResult: {
            loading: false,
            error: action.payload.error,
          },
        };
      default:
        return state;
    }
  };
}
