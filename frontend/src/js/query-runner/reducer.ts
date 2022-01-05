import { getType } from "typesafe-actions";

import type {
  ColumnDescription,
  DatasetIdT,
  GetQueryResponseDoneT,
} from "../api/types";
import type { Action } from "../app/actions";

import {
  queryResultErrorAction,
  queryResultReset,
  queryResultRunning,
  queryResultStart,
  queryResultSuccess,
  QueryTypeT,
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
  resultCount?: number | null;
  resultUrls?: string[];
  resultColumns?: ColumnDescription[] | null;
  queryType?: "CONCEPT_QUERY" | "SECONDARY_ID_QUERY";
}

export interface QueryRunnerStateT {
  runningQuery: string | null;
  progress?: number;
  queryRunning: boolean;
  startQuery: APICallType;
  stopQuery: APICallType;
  queryResult: QueryResultT | null;
}

export default function createQueryRunnerReducer(type: QueryTypeT) {
  const initialState: QueryRunnerStateT = {
    runningQuery: null,
    queryRunning: false,
    startQuery: {},
    stopQuery: {},
    queryResult: null,
  };

  const queryTypeMatches = (action: { payload: { queryType: QueryTypeT } }) => {
    return action.payload.queryType === type;
  };

  const getQueryResult = (
    data: GetQueryResponseDoneT,
    datasetId: DatasetIdT,
  ) => {
    return {
      datasetId,
      loading: false,
      success: true,
      error: null,
      resultCount: data.numberOfResults,
      resultUrls: data.resultUrls,
      resultColumns: data.columnDescriptions,
      queryType: data.queryType,
    };
  };

  return (
    state: QueryRunnerStateT = initialState,
    action: Action,
  ): QueryRunnerStateT => {
    switch (action.type) {
      case getType(startQuery.request):
        return queryTypeMatches(action)
          ? {
              ...state,
              stopQuery: {},
              startQuery: { loading: true },
              queryResult: null,
            }
          : state;
      case getType(startQuery.success):
        return queryTypeMatches(action)
          ? {
              ...state,
              runningQuery: action.payload.data.id,
              queryRunning: true,
              stopQuery: {},
              startQuery: { success: true },
            }
          : state;
      case getType(startQuery.failure):
        return queryTypeMatches(action)
          ? {
              ...state,
              stopQuery: {},
              startQuery: {
                error: action.payload.message || action.payload.status,
              },
            }
          : state;
      // To cancel a query
      case getType(stopQuery.request):
        return queryTypeMatches(action)
          ? { ...state, startQuery: {}, stopQuery: { loading: true } }
          : state;
      case getType(stopQuery.success):
        return queryTypeMatches(action)
          ? {
              ...state,
              runningQuery: null,
              progress: undefined,
              queryRunning: false,
              startQuery: {},
              stopQuery: { success: true },
            }
          : state;
      case getType(stopQuery.failure):
        return queryTypeMatches(action)
          ? {
              ...state,
              startQuery: {},
              stopQuery: {
                error: action.payload.message || action.payload.status,
              },
            }
          : state;

      // To check for query results
      case getType(queryResultStart):
        return queryTypeMatches(action)
          ? { ...state, queryResult: { loading: true } }
          : state;
      case getType(queryResultRunning):
        return queryTypeMatches(action)
          ? { ...state, progress: action.payload.progress }
          : state;
      case getType(queryResultReset):
        return queryTypeMatches(action)
          ? { ...state, queryResult: { loading: false } }
          : state;
      case getType(queryResultSuccess):
        if (!queryTypeMatches(action)) return state;

        const queryResult = getQueryResult(
          action.payload.data,
          action.payload.datasetId,
        );

        return {
          ...state,
          queryResult,
          runningQuery: null,
          progress: undefined,
          queryRunning: false,
        };
      case getType(queryResultErrorAction):
        if (!queryTypeMatches(action)) return state;

        const { payload } = action;

        return {
          ...state,
          runningQuery: null,
          progress: undefined,
          queryRunning: false,
          queryResult: {
            loading: false,
            error: payload.error,
          },
        };
      default:
        return state;
    }
  };
}
