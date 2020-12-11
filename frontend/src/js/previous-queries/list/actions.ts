import T from "i18n-react";
import { DatasetIdT } from "../../api/types";
import api from "../../api";

import { defaultSuccess, defaultError } from "../../common/actions";

import { setMessage } from "../../snack-message/actions";

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
  TOGGLE_SHARE_PREVIOUS_QUERY_SUCCESS,
  DELETE_PREVIOUS_QUERY_SUCCESS,
} from "./actionTypes";
import { PreviousQueryIdT } from "./reducer";

export const loadPreviousQueriesStart = () => ({
  type: LOAD_PREVIOUS_QUERIES_START,
});
export const loadPreviousQueriesSuccess = (res) =>
  defaultSuccess(LOAD_PREVIOUS_QUERIES_SUCCESS, res);
export const loadPreviousQueriesError = (err) =>
  defaultError(LOAD_PREVIOUS_QUERIES_ERROR, err);

export const loadPreviousQueries = (datasetId) => {
  return async (dispatch) => {
    dispatch(loadPreviousQueriesStart());

    try {
      const result = await api.getStoredQueries(datasetId);

      return dispatch(loadPreviousQueriesSuccess(result));
    } catch (e) {
      dispatch(setMessage("previousQueries.error"));

      return dispatch(loadPreviousQueriesError(e));
    }
  };
};

export const loadPreviousQueryStart = (queryId) => ({
  type: LOAD_PREVIOUS_QUERY_START,
  payload: { queryId },
});
export const loadPreviousQuerySuccess = (queryId, res) =>
  defaultSuccess(LOAD_PREVIOUS_QUERY_SUCCESS, res, { queryId });
export const loadPreviousQueryError = (queryId, err) =>
  defaultError(LOAD_PREVIOUS_QUERY_ERROR, err, { queryId });

export const loadPreviousQuery = (
  datasetId: DatasetIdT,
  queryId: PreviousQueryIdT
) => {
  return (dispatch) => {
    dispatch(loadPreviousQueryStart(queryId));

    return api.getStoredQuery(datasetId, queryId).then(
      (r) => dispatch(loadPreviousQuerySuccess(queryId, r)),
      (e) =>
        dispatch(
          loadPreviousQueryError(queryId, {
            message: T.translate("previousQuery.loadError"),
          })
        )
    );
  };
};

export const toggleEditPreviousQueryLabel = (queryId) => ({
  type: TOGGLE_EDIT_PREVIOUS_QUERY_LABEL,
  payload: { queryId },
});

export const renamePreviousQueryStart = (queryId) => ({
  type: RENAME_PREVIOUS_QUERY_START,
  payload: { queryId },
});
export const renamePreviousQuerySuccess = (queryId, label, res) =>
  defaultSuccess(RENAME_PREVIOUS_QUERY_SUCCESS, res, { queryId, label });
export const renamePreviousQueryError = (queryId, err) =>
  defaultError(RENAME_PREVIOUS_QUERY_ERROR, err, { queryId });

export const renamePreviousQuery = (datasetId, queryId, label) => {
  return (dispatch) => {
    dispatch(renamePreviousQueryStart(queryId));

    return api.patchStoredQuery(datasetId, queryId, { label }).then(
      (r) => {
        dispatch(renamePreviousQuerySuccess(queryId, label, r));
        dispatch(toggleEditPreviousQueryLabel(queryId));
      },
      (e) =>
        dispatch(
          renamePreviousQueryError(queryId, {
            message: T.translate("previousQuery.renameError"),
          })
        )
    );
  };
};

export const toggleEditPreviousQueryTags = (queryId) => ({
  type: TOGGLE_EDIT_PREVIOUS_QUERY_TAGS,
  payload: { queryId },
});

export const retagPreviousQueryStart = (queryId) => ({
  type: RETAG_PREVIOUS_QUERY_START,
  payload: { queryId },
});
export const retagPreviousQuerySuccess = (queryId, tags, res) =>
  defaultSuccess(RETAG_PREVIOUS_QUERY_SUCCESS, res, { queryId, tags });
export const retagPreviousQueryError = (queryId, err) =>
  defaultError(RETAG_PREVIOUS_QUERY_ERROR, err, { queryId });

export const retagPreviousQuery = (datasetId, queryId, tags) => {
  return (dispatch) => {
    dispatch(retagPreviousQueryStart(queryId));

    return api.patchStoredQuery(datasetId, queryId, { tags }).then(
      (r) => {
        dispatch(retagPreviousQuerySuccess(queryId, tags, r));
        dispatch(toggleEditPreviousQueryTags(queryId));
      },
      (e) =>
        dispatch(
          retagPreviousQueryError(queryId, {
            message: T.translate("previousQuery.retagError"),
          })
        )
    );
  };
};

export const sharePreviousQuerySuccess = (
  queryId: string,
  shared: boolean,
  groups: PreviousQueryIdT[]
) =>
  defaultSuccess(TOGGLE_SHARE_PREVIOUS_QUERY_SUCCESS, null, {
    queryId,
    shared,
    groups,
  });

export const deletePreviousQuerySuccess = (queryId: string) =>
  defaultSuccess(DELETE_PREVIOUS_QUERY_SUCCESS, null, { queryId });
