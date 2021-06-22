import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

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
import { ErrorObject, errorPayload } from "../../common/actions";
import { useDatasetId } from "../../dataset/selectors";
import { setMessage } from "../../snack-message/actions";

import { PreviousQueryIdT } from "./reducer";

export type PreviousQueryListActions = ActionType<
  | typeof loadPreviousQueries
  | typeof loadPreviousQuery
  | typeof renamePreviousQuery
  | typeof retagPreviousQuery
  | typeof sharePreviousQuerySuccess
  | typeof deletePreviousQuerySuccess
>;

export const loadPreviousQueries = createAsyncAction(
  "previous-queries/LOAD_PREVIOUS_QUERIES_START",
  "previous-queries/LOAD_PREVIOUS_QUERIES_SUCCESS",
  "previous-queries/LOAD_PREVIOUS_QUERIES_ERROR",
)<undefined, { data: GetStoredQueriesResponseT }, ErrorObject>();

export const useLoadPreviousQueries = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const getStoredQueries = useGetStoredQueries();

  return async (datasetId: DatasetIdT) => {
    dispatch(loadPreviousQueries.request());

    try {
      const data = await getStoredQueries(datasetId);

      return dispatch(loadPreviousQueries.success({ data }));
    } catch (e) {
      dispatch(setMessage({ message: t("previousQueries.error") }));

      return dispatch(loadPreviousQueries.failure(errorPayload(e, {})));
    }
  };
};

interface QueryContext {
  queryId: QueryIdT;
}
export const loadPreviousQuery = createAsyncAction(
  "previous-queries/LOAD_PREVIOUS_QUERY_START",
  "previous-queries/LOAD_PREVIOUS_QUERY_SUCCESS",
  "previous-queries/LOAD_PREVIOUS_QUERY_ERROR",
)<
  QueryContext,
  QueryContext & { data: GetStoredQueryResponseT },
  QueryContext & ErrorObject
>();

export const useLoadPreviousQuery = () => {
  const dispatch = useDispatch();
  const getStoredQuery = useGetStoredQuery();
  const { t } = useTranslation();

  return (datasetId: DatasetIdT, queryId: PreviousQueryIdT) => {
    dispatch(loadPreviousQuery.request({ queryId }));

    return getStoredQuery(datasetId, queryId).then(
      (r) => dispatch(loadPreviousQuery.success({ queryId, data: r })),
      () =>
        dispatch(
          loadPreviousQuery.failure({
            queryId,
            message: t("previousQuery.loadError"),
          }),
        ),
    );
  };
};

export const renamePreviousQuery = createAsyncAction(
  "previous-queries/RENAME_PREVIOUS_QUERY_START",
  "previous-queries/RENAME_PREVIOUS_QUERY_SUCCESS",
  "previous-queries/RENAME_PREVIOUS_QUERY_ERROR",
)<QueryContext, QueryContext & { label: string }, QueryContext & ErrorObject>();

export const useRenamePreviousQuery = () => {
  const dispatch = useDispatch();
  const patchStoredQuery = usePatchStoredQuery();
  const { t } = useTranslation();

  return (datasetId: DatasetIdT, queryId: PreviousQueryIdT, label: string) => {
    dispatch(renamePreviousQuery.request({ queryId }));

    return patchStoredQuery(datasetId, queryId, { label }).then(
      () => dispatch(renamePreviousQuery.success({ queryId, label })),
      () =>
        dispatch(
          renamePreviousQuery.failure({
            queryId,
            message: t("previousQuery.renameError"),
          }),
        ),
    );
  };
};

export const retagPreviousQuery = createAsyncAction(
  "previous-queries/RETAG_PREVIOUS_QUERY_START",
  "previous-queries/RETAG_PREVIOUS_QUERY_SUCCESS",
  "previous-queries/RETAG_PREVIOUS_QUERY_ERROR",
)<
  QueryContext,
  QueryContext & { tags: string[] },
  QueryContext & ErrorObject
>();

export const useRetagPreviousQuery = () => {
  const dispatch = useDispatch();
  const patchStoredQuery = usePatchStoredQuery();
  const datasetId = useDatasetId();
  const { t } = useTranslation();

  return (queryId: PreviousQueryIdT, tags: string[]) => {
    if (!datasetId) {
      return Promise.resolve();
    }

    dispatch(retagPreviousQuery.request({ queryId }));

    return patchStoredQuery(datasetId, queryId, { tags }).then(
      () => {
        dispatch(retagPreviousQuery.success({ queryId, tags }));
      },
      () =>
        dispatch(
          retagPreviousQuery.failure({
            queryId,
            message: t("previousQuery.retagError"),
          }),
        ),
    );
  };
};

export const sharePreviousQuerySuccess = createAction(
  "previous-queries/TOGGLE_SHARE_PREVIOUS_QUERY_SUCCESS",
)<{
  queryId: string;
  groups: PreviousQueryIdT[];
}>();

export const deletePreviousQuerySuccess = createAction(
  "previous-queries/DELETE_PREVIOUS_QUERY_SUCCESS",
)<{ queryId: string }>();
