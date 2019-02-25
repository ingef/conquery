// @flow

import T                         from 'i18n-react';
import { toUpperCaseUnderscore } from '../common/helpers';
import * as actionTypes          from './actionTypes';

type APICallType = {
  loading?: boolean,
  success?: boolean,
  error?: string,
};

export type StateType = {
  runningQuery: ?(number | string),
  queryRunning: boolean,
  startQuery: APICallType,
  stopQuery: APICallType,
  queryResult:APICallType,
};

export default function createQueryRunnerReducer(type: string): Function {
  const initialState: StateType = {
    runningQuery: null,
    queryRunning: false,
    startQuery: Object,
    stopQuery: Object,
    queryResult: Object,
  };

  const capitalType = toUpperCaseUnderscore(type);

  // Example1: START_STANDARD_QUERY_START
  // Example2: START_TIMEBASED_QUERY_START
  const START_QUERY_START = actionTypes[`START_${capitalType}_QUERY_START`];
  const START_QUERY_SUCCESS = actionTypes[`START_${capitalType}_QUERY_SUCCESS`];
  const START_QUERY_ERROR = actionTypes[`START_${capitalType}_QUERY_ERROR`];
  const STOP_QUERY_START = actionTypes[`STOP_${capitalType}_QUERY_START`];
  const STOP_QUERY_SUCCESS = actionTypes[`STOP_${capitalType}_QUERY_SUCCESS`];
  const STOP_QUERY_ERROR = actionTypes[`STOP_${capitalType}_QUERY_ERROR`];
  const QUERY_RESULT_START = actionTypes[`QUERY_${capitalType}_RESULT_START`];
  const QUERY_RESULT_STOP = actionTypes[`QUERY_${capitalType}_RESULT_STOP`];
  const QUERY_RESULT_SUCCESS = actionTypes[`QUERY_${capitalType}_RESULT_SUCCESS`];
  const QUERY_RESULT_ERROR = actionTypes[`QUERY_${capitalType}_RESULT_ERROR`];

  const getQueryResult = (data) => {
    if (data.status === 'CANCELED')
      return { loading: false, error: T.translate('queryRunner.queryCanceled') };
    else if (data.status === 'FAILED')
      return { loading: false, error: T.translate('queryRunner.queryFailed') };

    // E.G. STATUS DONE
    return {
      loading: false,
      resultCount: data.numberOfResults,
      resultUrl: data.resultUrl
    };
  };

  return (state: StateType = initialState, action: Object): StateType => {
    switch (action.type) {
      // To start a query
      case START_QUERY_START:
        return { ...state, stopQuery: {}, startQuery: { loading: true }, queryResult: {} };
      case START_QUERY_SUCCESS:
        return {
          ...state,
          runningQuery: action.payload.data.id,
          queryRunning: true,
          stopQuery: {},
          startQuery: { success: true }
        };
      case START_QUERY_ERROR:
        return { ...state, stopQuery: {}, startQuery: { error: action.payload.message } };

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
        return { ...state, startQuery: {}, stopQuery: { error: action.payload.message } };

      // To check for query results
      case QUERY_RESULT_START:
        return { ...state, queryResult: { loading: true } };
      case QUERY_RESULT_STOP:
        return { ...state, queryResult: { loading: false } };
      case QUERY_RESULT_SUCCESS:
        const { data } = action.payload;

        const queryResult = getQueryResult(data);

        return {
          ...state,
          queryResult,
          runningQuery: null,
          queryRunning: false
        };
      case QUERY_RESULT_ERROR:
        return {
          ...state,
          runningQuery: null,
          queryRunning: false,
          queryResult: {
            loading: false,
            error: action.payload.message || T.translate('queryRunner.queryFailed')
          }
        };
      default:
        return state;
    }
  };
};
