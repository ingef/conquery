import type {
  ColumnDescription,
  DatasetIdT,
  GetQueryResponseDoneT,
} from "../api/types";

import {
  QUERY_RESULT_ERROR,
  QUERY_RESULT_RESET,
  QUERY_RESULT_RUNNING,
  QUERY_RESULT_START,
  QUERY_RESULT_SUCCESS,
  START_QUERY_ERROR,
  START_QUERY_START,
  START_QUERY_SUCCESS,
  STOP_QUERY_ERROR,
  STOP_QUERY_START,
  STOP_QUERY_SUCCESS,
} from "./actionTypes";
import { QueryTypeT } from "./actions";

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
    action: Object,
  ): QueryRunnerStateT => {
    if (!action.payload || action.payload.queryType !== type) {
      return state;
    }

    switch (action.type) {
      // To start a query
      case START_QUERY_START:
        return {
          ...state,
          stopQuery: {},
          startQuery: { loading: true },
          queryResult: null,
        };
      case START_QUERY_SUCCESS:
        return {
          ...state,
          runningQuery: action.payload.data.id,
          queryRunning: true,
          stopQuery: {},
          startQuery: { success: true },
        };
      case START_QUERY_ERROR:
        return {
          ...state,
          stopQuery: {},
          startQuery: {
            error: action.payload.message || action.payload.status,
          },
        };

      // To cancel a query
      case STOP_QUERY_START:
        return { ...state, startQuery: {}, stopQuery: { loading: true } };
      case STOP_QUERY_SUCCESS:
        return {
          ...state,
          runningQuery: null,
          progress: undefined,
          queryRunning: false,
          startQuery: {},
          stopQuery: { success: true },
        };
      case STOP_QUERY_ERROR:
        return {
          ...state,
          startQuery: {},
          stopQuery: { error: action.payload.message || action.payload.status },
        };

      // To check for query results
      case QUERY_RESULT_START:
        return { ...state, queryResult: { loading: true } };
      case QUERY_RESULT_RUNNING:
        return { ...state, progress: action.payload.progress };
      case QUERY_RESULT_RESET:
        return { ...state, queryResult: { loading: false } };
      case QUERY_RESULT_SUCCESS:
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
      case QUERY_RESULT_ERROR:
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
