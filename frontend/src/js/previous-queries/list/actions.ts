import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import {
  useGetStoredQueries,
  usePatchStoredQuery,
  useGetStoredQuery,
} from "../../api/api";
import {
  DatasetIdT,
  GetStoredQueriesResponseT,
  GetStoredQueryResponseT,
  QueryIdT,
} from "../../api/types";
import {
  defaultSuccess,
  defaultError,
  ErrorObject,
} from "../../common/actions";
import { useDatasetId } from "../../dataset/selectors";
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
export const loadPreviousQueriesSuccess = (res: GetStoredQueriesResponseT) =>
  defaultSuccess(LOAD_PREVIOUS_QUERIES_SUCCESS, res);
export const loadPreviousQueriesError = (err: ErrorObject) =>
  defaultError(LOAD_PREVIOUS_QUERIES_ERROR, err);

export const useLoadPreviousQueries = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const getStoredQueries = useGetStoredQueries();

  return async (datasetId: DatasetIdT) => {
    dispatch(loadPreviousQueriesStart());

    try {
      const result = await getStoredQueries(datasetId);

      return dispatch(loadPreviousQueriesSuccess(result));
    } catch (e) {
      dispatch(setMessage({ message: t("previousQueries.error") }));

      return dispatch(loadPreviousQueriesError(e));
    }
  };
};

export const loadPreviousQueryStart = (queryId: QueryIdT) => ({
  type: LOAD_PREVIOUS_QUERY_START,
  payload: { queryId },
});
export const loadPreviousQuerySuccess = (
  queryId: QueryIdT,
  res: GetStoredQueryResponseT,
) => defaultSuccess(LOAD_PREVIOUS_QUERY_SUCCESS, res, { queryId });
export const loadPreviousQueryError = (queryId: QueryIdT, err: ErrorObject) =>
  defaultError(LOAD_PREVIOUS_QUERY_ERROR, err, { queryId });

export const useLoadPreviousQuery = () => {
  const dispatch = useDispatch();
  const getStoredQuery = useGetStoredQuery();
  const { t } = useTranslation();

  return (datasetId: DatasetIdT, queryId: PreviousQueryIdT) => {
    dispatch(loadPreviousQueryStart(queryId));

    return getStoredQuery(datasetId, queryId).then(
      (r) => dispatch(loadPreviousQuerySuccess(queryId, r)),
      (e) =>
        dispatch(
          loadPreviousQueryError(queryId, {
            message: t("previousQuery.loadError"),
          }),
        ),
    );
  };
};

export const renamePreviousQueryStart = (queryId: QueryIdT) => ({
  type: RENAME_PREVIOUS_QUERY_START,
  payload: { queryId },
});
export const renamePreviousQuerySuccess = (
  queryId: QueryIdT,
  label: string,
  res: any,
) => defaultSuccess(RENAME_PREVIOUS_QUERY_SUCCESS, res, { queryId, label });
export const renamePreviousQueryError = (queryId: QueryIdT, err: ErrorObject) =>
  defaultError(RENAME_PREVIOUS_QUERY_ERROR, err, { queryId });

export const useRenamePreviousQuery = () => {
  const dispatch = useDispatch();
  const patchStoredQuery = usePatchStoredQuery();
  const { t } = useTranslation();

  return (datasetId: DatasetIdT, queryId: PreviousQueryIdT, label: string) => {
    dispatch(renamePreviousQueryStart(queryId));

    return patchStoredQuery(datasetId, queryId, { label }).then(
      (r) => dispatch(renamePreviousQuerySuccess(queryId, label, r)),
      (e) =>
        dispatch(
          renamePreviousQueryError(queryId, {
            message: t("previousQuery.renameError"),
          }),
        ),
    );
  };
};

export const retagPreviousQueryStart = (queryId: QueryIdT) => ({
  type: RETAG_PREVIOUS_QUERY_START,
  payload: { queryId },
});
export const retagPreviousQuerySuccess = (
  queryId: QueryIdT,
  tags: string[],
  res: any,
) => defaultSuccess(RETAG_PREVIOUS_QUERY_SUCCESS, res, { queryId, tags });
export const retagPreviousQueryError = (queryId: QueryIdT, err: ErrorObject) =>
  defaultError(RETAG_PREVIOUS_QUERY_ERROR, err, { queryId });

export const useRetagPreviousQuery = () => {
  const dispatch = useDispatch();
  const patchStoredQuery = usePatchStoredQuery();
  const datasetId = useDatasetId();
  const { t } = useTranslation();

  return (queryId: PreviousQueryIdT, tags: string[]) => {
    if (!datasetId) {
      return Promise.resolve();
    }

    dispatch(retagPreviousQueryStart(queryId));

    return patchStoredQuery(datasetId, queryId, { tags }).then(
      (r) => {
        dispatch(retagPreviousQuerySuccess(queryId, tags, r));
      },
      (e) =>
        dispatch(
          retagPreviousQueryError(queryId, {
            message: t("previousQuery.retagError"),
          }),
        ),
    );
  };
};

export const sharePreviousQuerySuccess = (
  queryId: string,
  groups: PreviousQueryIdT[],
) =>
  defaultSuccess(TOGGLE_SHARE_PREVIOUS_QUERY_SUCCESS, null, {
    queryId,
    groups,
  });

export const deletePreviousQuerySuccess = (queryId: string) =>
  defaultSuccess(DELETE_PREVIOUS_QUERY_SUCCESS, null, { queryId });
