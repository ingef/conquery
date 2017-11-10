// @flow

import api from '../api';

import {
  defaultSuccess,
  defaultError
} from '../common/actions';

import {
  capitalize
} from '../common/helpers';

import {
  loadPreviousQueries
} from '../previous-queries/list/actions';

import * as actionTypes from './actionTypes';
import { QUERY_AGAIN_TIMEOUT } from './constants';


export default function createQueryRunnerActions(
  type: string,
  isForm: boolean = false
): { [string]: Function } {
  const uppercaseType = type.toUpperCase();
  const capitalizedType = capitalize(type);

  const START_QUERY_START = actionTypes[`START_${uppercaseType}_QUERY_START`];
  const START_QUERY_SUCCESS = actionTypes[`START_${uppercaseType}_QUERY_SUCCESS`];
  const START_QUERY_ERROR = actionTypes[`START_${uppercaseType}_QUERY_ERROR`];
  const STOP_QUERY_START = actionTypes[`STOP_${uppercaseType}_QUERY_START`];
  const STOP_QUERY_SUCCESS = actionTypes[`STOP_${uppercaseType}_QUERY_SUCCESS`];
  const STOP_QUERY_ERROR = actionTypes[`STOP_${uppercaseType}_QUERY_ERROR`];
  const QUERY_RESULT_START = actionTypes[`QUERY_${uppercaseType}_RESULT_START`];
  const QUERY_RESULT_STOP = actionTypes[`QUERY_${uppercaseType}_RESULT_STOP`];
  const QUERY_RESULT_SUCCESS = actionTypes[`QUERY_${uppercaseType}_RESULT_SUCCESS`];
  const QUERY_RESULT_ERROR = actionTypes[`QUERY_${uppercaseType}_RESULT_ERROR`];

  const startQueryStart = () => ({ type: START_QUERY_START });
  const startQueryError = (err) => defaultError(START_QUERY_ERROR, err);
  const startQuerySuccess = (res) => defaultSuccess(START_QUERY_SUCCESS, res);
  const startQuery = (datasetId, query, version) => {
    return (dispatch) => {
      dispatch(startQueryStart());

      const apiMethod = isForm ? api.postFormQueries : api.postQueries;

      return apiMethod(datasetId, query, type, version)
        .then(
          r => {
            dispatch(startQuerySuccess(r));

            const queryId = r.id;

            return dispatch(queryResult(datasetId, queryId));
          },
          e => dispatch(startQueryError(e))
        );
    };
  };

  const stopQueryStart = () => ({ type: STOP_QUERY_START });
  const stopQueryError = (err) => defaultError(STOP_QUERY_ERROR, err);
  const stopQuerySuccess = (res) => defaultSuccess(STOP_QUERY_SUCCESS, res);
  const stopQuery = (datasetId, queryId) => {
    return (dispatch) => {
      dispatch(stopQueryStart());

      const apiMethod = isForm ? api.deleteFormQuery : api.deleteQuery;

      return apiMethod(datasetId, queryId)
        .then(
          r => dispatch(stopQuerySuccess(r)),
          e => dispatch(stopQueryError(e))
        );
    };
  };

  const queryResultStart = () => ({ type: QUERY_RESULT_START });
  const queryResultStop = () => ({ type: QUERY_RESULT_STOP });
  const queryResultError = (err) => defaultError(QUERY_RESULT_ERROR, err);
  const queryResultSuccess = (res) => defaultSuccess(QUERY_RESULT_SUCCESS, res);
  const queryResult = (datasetId, queryId) => {
    return (dispatch) => {
      dispatch(queryResultStart());

      const apiMethod = isForm ? api.getFormQuery : api.getQuery;

      return apiMethod(datasetId, queryId)
        .then(
          r => {
            // Indicate that looking for the result has stopped,
            // but not necessarily succeeded
            dispatch(queryResultStop());

            if (r.status === 'DONE') {
              dispatch(queryResultSuccess(r));

              // Now there should be a new result that can be queried
              dispatch(loadPreviousQueries(datasetId));
            } else if (r.status === 'CANCELED' || r.status === 'FAILED') {
              dispatch(queryResultError(r));
            } else {
              // Try again after a short time:
              //   Use the "long polling" strategy, where we assume that the
              //   backend blocks the request for a couple of seconds and waits
              //   for the query comes back.
              //   If it doesn't come back the request resolves and
              //   we - the frontend - try again almost instantly.
              setTimeout(
                () => dispatch(queryResult(datasetId, queryId)),
                QUERY_AGAIN_TIMEOUT
              );
            }
          },
          e => dispatch(queryResultError(e))
        );
    };
  };

  return {
    [`start${capitalizedType}QueryStart`]: startQueryStart,
    [`start${capitalizedType}QueryError`]: startQueryError,
    [`start${capitalizedType}QuerySuccess`]: startQuerySuccess,
    [`start${capitalizedType}Query`]: startQuery,
    [`stop${capitalizedType}QueryStart`]: stopQueryStart,
    [`stop${capitalizedType}QueryError`]: stopQueryError,
    [`stop${capitalizedType}QuerySuccess`]: stopQuerySuccess,
    [`stop${capitalizedType}Query`]: stopQuery,
    [`query${capitalizedType}ResultStart`]: queryResultStart,
    [`query${capitalizedType}ResultStop`]: queryResultStop,
    [`query${capitalizedType}ResultError`]: queryResultError,
    [`query${capitalizedType}ResultSuccess`]: queryResultSuccess,
    [`query${capitalizedType}Result`]: queryResult,
  };
}
