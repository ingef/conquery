import T        from 'i18n-react';
import api      from '../../api';

import {
  defaultSuccess,
  defaultError
} from '../../common/actions';

import {
  LOAD_PREVIOUS_QUERIES_START,
  LOAD_PREVIOUS_QUERIES_SUCCESS,
  LOAD_PREVIOUS_QUERIES_ERROR,
  LOAD_PREVIOUS_QUERY_START,
  LOAD_PREVIOUS_QUERY_SUCCESS,
  LOAD_PREVIOUS_QUERY_ERROR,
  RENAME_PREVIOUS_QUERY_START,
  RENAME_PREVIOUS_QUERY_SUCCESS,
  RENAME_PREVIOUS_QUERY_ERROR,
  TOGGLE_EDIT_PREVIOUS_QUERY_LABEL,
  TOGGLE_EDIT_PREVIOUS_QUERY_TAGS,
  RETAG_PREVIOUS_QUERY_START,
  RETAG_PREVIOUS_QUERY_SUCCESS,
  RETAG_PREVIOUS_QUERY_ERROR,
  SHARE_PREVIOUS_QUERY_START,
  SHARE_PREVIOUS_QUERY_SUCCESS,
  SHARE_PREVIOUS_QUERY_ERROR,
  DELETE_PREVIOUS_QUERY_START,
  DELETE_PREVIOUS_QUERY_SUCCESS,
  DELETE_PREVIOUS_QUERY_ERROR,
} from './actionTypes';

export const loadPreviousQueriesStart = () =>
  ({ type: LOAD_PREVIOUS_QUERIES_START });
export const loadPreviousQueriesSuccess = (res) =>
  defaultSuccess(LOAD_PREVIOUS_QUERIES_SUCCESS, res);
export const loadPreviousQueriesError = (err) =>
  defaultError(LOAD_PREVIOUS_QUERIES_ERROR, err);

export const loadPreviousQueries = (datasetId) => {
  return (dispatch) => {
    dispatch(loadPreviousQueriesStart());

    return api.getStoredQueries(datasetId)
      .then(
        r => dispatch(loadPreviousQueriesSuccess(r)),
        e => dispatch(loadPreviousQueriesError(e))
      );
  };
};

export const loadPreviousQueryStart = (queryId) =>
  ({ type: LOAD_PREVIOUS_QUERY_START, payload: { queryId } });
export const loadPreviousQuerySuccess = (queryId, res) =>
  defaultSuccess(LOAD_PREVIOUS_QUERY_SUCCESS, res, { queryId });
export const loadPreviousQueryError = (queryId, err) =>
  defaultError(LOAD_PREVIOUS_QUERY_ERROR, err, { queryId });

export const loadPreviousQuery = (datasetId, queryId) => {
  return (dispatch) => {
    dispatch(loadPreviousQueryStart(queryId));

    return api.getStoredQuery(datasetId, queryId)
      .then(
        r => dispatch(loadPreviousQuerySuccess(queryId, r)),
        e => dispatch(loadPreviousQueryError(queryId, {
          message: T.translate('previousQuery.loadError')
        }))
      );
  };
};


export const toggleEditPreviousQueryLabel = (queryId) => ({
  type: TOGGLE_EDIT_PREVIOUS_QUERY_LABEL,
  payload: { queryId },
});

export const renamePreviousQueryStart = (queryId) =>
  ({ type: RENAME_PREVIOUS_QUERY_START, payload: { queryId } });
export const renamePreviousQuerySuccess = (queryId, label, res) =>
  defaultSuccess(RENAME_PREVIOUS_QUERY_SUCCESS, res, { queryId, label });
export const renamePreviousQueryError = (queryId, err) =>
  defaultError(RENAME_PREVIOUS_QUERY_ERROR, err, { queryId });

export const renamePreviousQuery = (datasetId, queryId, label) => {
  return (dispatch) => {
    dispatch(renamePreviousQueryStart(queryId));

    return api.patchStoredQuery(datasetId, queryId, { label })
      .then(
        r => {
          dispatch(renamePreviousQuerySuccess(queryId, label, r));
          dispatch(toggleEditPreviousQueryLabel(queryId));
        },
        e => dispatch(renamePreviousQueryError(queryId, {
          message: T.translate('previousQuery.renameError')
        }))
      );
  };
};

export const toggleEditPreviousQueryTags = (queryId) => ({
  type: TOGGLE_EDIT_PREVIOUS_QUERY_TAGS,
  payload: { queryId },
});

export const retagPreviousQueryStart = (queryId) =>
  ({ type: RETAG_PREVIOUS_QUERY_START, payload: { queryId } });
export const retagPreviousQuerySuccess = (queryId, tags, res) =>
  defaultSuccess(RETAG_PREVIOUS_QUERY_SUCCESS, res, { queryId, tags });
export const retagPreviousQueryError = (queryId, err) =>
  defaultError(RETAG_PREVIOUS_QUERY_ERROR, err, { queryId });

export const retagPreviousQuery = (datasetId, queryId, tags) => {
  return (dispatch) => {
    dispatch(retagPreviousQueryStart(queryId));

    return api.patchStoredQuery(datasetId, queryId, { tags })
      .then(
        r => {
          dispatch(retagPreviousQuerySuccess(queryId, tags, r));
          dispatch(toggleEditPreviousQueryTags(queryId));
        },
        e => dispatch(retagPreviousQueryError(queryId, {
          message: T.translate('previousQuery.retagError')
        }))
      );
  };
};

export const sharePreviousQueryStart = (queryId) =>
  ({ type: SHARE_PREVIOUS_QUERY_START, payload: { queryId } });
export const sharePreviousQuerySuccess = (queryId, res) =>
  defaultSuccess(SHARE_PREVIOUS_QUERY_SUCCESS, res, { queryId, shared: true });
export const sharePreviousQueryError = (queryId, err) =>
  defaultError(SHARE_PREVIOUS_QUERY_ERROR, err, { queryId });

export const sharePreviousQuery = (datasetId, queryId) => {
  return (dispatch) => {
    dispatch(sharePreviousQueryStart(queryId));

    return api.patchStoredQuery(datasetId, queryId, { shared: true })
      .then(
        r => dispatch(sharePreviousQuerySuccess(queryId, r)),
        e => dispatch(sharePreviousQueryError(queryId, {
          message: T.translate('previousQuery.shareError')
        }))
      );
  };
};

export const deletePreviousQueryStart = (queryId) =>
  ({ type: DELETE_PREVIOUS_QUERY_START, payload: { queryId } });
export const deletePreviousQuerySuccess = (queryId, res) =>
  defaultSuccess(DELETE_PREVIOUS_QUERY_SUCCESS, res, { queryId });
export const deletePreviousQueryError = (queryId, err) =>
  defaultError(DELETE_PREVIOUS_QUERY_ERROR, err, { queryId });

export const deletePreviousQuery = (datasetId, queryId) => {
  return (dispatch) => {
    dispatch(deletePreviousQueryStart(queryId));

    return api.deleteStoredQuery(datasetId, queryId)
      .then(
        r => dispatch(deletePreviousQuerySuccess(queryId, r)),
        e => dispatch(deletePreviousQueryError(queryId, {
          message: T.translate('previousQuery.deleteError')
        }))
      );
  };
};
