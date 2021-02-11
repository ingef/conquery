import type {
  ColumnDescription,
  DatasetIdT,
  GetQueryResponseDoneT,
  ErrorResponseT,
} from "../api/types";
import { getErrorCodeMessageKey } from "../api/errorCodes";
import { QueryTypeT } from "./actions";
import {
  QUERY_RESULT_ERROR,
  QUERY_RESULT_RESET,
  QUERY_RESULT_START,
  QUERY_RESULT_SUCCESS,
  START_QUERY_ERROR,
  START_QUERY_START,
  START_QUERY_SUCCESS,
  STOP_QUERY_ERROR,
  STOP_QUERY_START,
  STOP_QUERY_SUCCESS,
} from "./actionTypes";

interface APICallType {
  loading?: boolean;
  success?: boolean;
  error?: string;
  errorContext?: Record<string, string>;
}

export interface QueryRunnerStateT {
  runningQuery: string | null;
  queryRunning: boolean;
  startQuery: APICallType;
  stopQuery: APICallType;
  queryResult:
    | (APICallType & {
        datasetId?: string;
        resultCount?: number;
        resultUrl?: string;
        resultColumns?: ColumnDescription[];
        queryType?: "CONCEPT_QUERY" | "SECONDARY_ID_QUERY";
      })
    | null;
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
    datasetId: DatasetIdT
  ) => {
    return {
      datasetId,
      loading: false,
      success: true,
      error: null,
      resultCount: data.numberOfResults,
      resultUrl: data.resultUrl,
      resultColumns: data.columnDescriptions,
      queryType: data.queryType,
    };
  };

  const getQueryError = ({
    status,
    error,
  }: {
    status: "CANCELED" | "FAILED";
    error: ErrorResponseT | null;
  }): string => {
    if (status === "CANCELED") {
      return "queryRunner.queryCanceled";
    }

    return (
      (error && error.code && getErrorCodeMessageKey(error.code)) ||
      "queryRunner.queryFailed"
    );
  };

  return (
    state: QueryRunnerStateT = initialState,
    action: Object
  ): QueryRunnerStateT => {
    if (action.queryType !== type) {
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
      case QUERY_RESULT_RESET:
        return { ...state, queryResult: { loading: false } };
      case QUERY_RESULT_SUCCESS:
        const queryResult = getQueryResult(
          action.payload.data,
          action.payload.datasetId
        );

        return {
          ...state,
          queryResult,
          runningQuery: null,
          queryRunning: false,
        };
      case QUERY_RESULT_ERROR:
        const { payload } = action;
        const error = getQueryError(payload);
        const errorContext = (payload.error && payload.error.context) || {};

        return {
          ...state,
          runningQuery: null,
          queryRunning: false,
          queryResult: {
            loading: false,
            error,
            errorContext,
          },
        };
      default:
        return state;
    }
  };
}
