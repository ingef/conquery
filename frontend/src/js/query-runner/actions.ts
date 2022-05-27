import { TFunction, useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import {
  FormQueryPostPayload,
  useGetQuery,
  usePostFormQueries,
  usePostQueries,
  usePostQueryCancel,
} from "../api/api";
import type {
  DatasetT,
  ErrorResponseT,
  GetQueryErrorResponseT,
  GetQueryResponseDoneT,
  PostQueriesResponseT,
  QueryIdT,
} from "../api/types";
import { ErrorObject, errorPayload, successPayload } from "../common/actions";
import { getExternalSupportedErrorMessage } from "../environment";
import {
  useLoadFormConfigs,
  useLoadQueries,
} from "../previous-queries/list/actions";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";
import type { ValidatedTimebasedQueryStateT } from "../timebased-query-editor/reducer";

import { QUERY_AGAIN_TIMEOUT } from "./constants";

export type QueryRunnerActions = ActionType<
  | typeof startQuery
  | typeof stopQuery
  | typeof queryResultStart
  | typeof queryResultReset
  | typeof queryResultRunning
  | typeof queryResultSuccess
  | typeof queryResultError
>;

/*
  This implements a polling mechanism,
  because queries are running sometimes longer than 10s.
  (we're using "long polling", where the backend delays the response)

  The polling works like:
  - Start the query (POST request)
    - From the response, get an ID when query was successfully started
  - Continuously poll (GET request) for the query results using that ID
  - Stop polling once the status is DONE, CANCELED or FAILED

  Also, there's a possibility to stop a query while it's running,
  by sending a DELETE request for that query ID
*/

export type QueryTypeT = "standard" | "timebased" | "externalForms";

export const startQuery = createAsyncAction(
  "query-runners/START_QUERY_START",
  "query-runners/START_QUERY_SUCCESS",
  "query-runners/START_QUERY_ERROR",
)<
  { queryType: QueryTypeT },
  { queryType: QueryTypeT; data: PostQueriesResponseT },
  ErrorObject & { queryType: QueryTypeT }
>();

export const useStartQuery = (queryType: QueryTypeT) => {
  const dispatch = useDispatch();
  const queryResult = useQueryResult(queryType);
  const postQueries = usePostQueries();
  const postFormQueries = usePostFormQueries();

  return (
    datasetId: DatasetT["id"],
    query:
      | StandardQueryStateT
      | ValidatedTimebasedQueryStateT
      | FormQueryPostPayload,
    {
      selectedSecondaryId,
    }: {
      selectedSecondaryId?: string | null;
    } = {},
  ) => {
    dispatch(startQuery.request({ queryType }));

    const apiMethod =
      queryType === "externalForms"
        ? () => postFormQueries(datasetId, query as FormQueryPostPayload)
        : () =>
            postQueries(
              datasetId,
              query as StandardQueryStateT | ValidatedTimebasedQueryStateT,
              {
                queryType,
                selectedSecondaryId,
              },
            );

    return apiMethod().then(
      (r) => {
        dispatch(startQuery.success(successPayload(r, { queryType })));

        const queryId = r.id;

        return queryResult(datasetId, queryId);
      },
      (e) => dispatch(startQuery.failure(errorPayload(e, { queryType }))),
    );
  };
};

export const stopQuery = createAsyncAction(
  "query-runners/STOP_QUERY_START",
  "query-runners/STOP_QUERY_SUCCESS",
  "query-runners/STOP_QUERY_ERROR",
)<
  { queryType: QueryTypeT },
  { queryType: QueryTypeT; data: null },
  ErrorObject & { queryType: QueryTypeT }
>();

export const useStopQuery = (queryType: QueryTypeT) => {
  const dispatch = useDispatch();
  const cancelQuery = usePostQueryCancel();

  return (datasetId: DatasetT["id"], queryId: QueryIdT) => {
    dispatch(stopQuery.request({ queryType }));

    return cancelQuery(datasetId, queryId).then(
      (r) => dispatch(stopQuery.success(successPayload(r, { queryType }))),
      (e) => dispatch(stopQuery.failure(errorPayload(e, { queryType }))),
    );
  };
};

export const queryResultStart = createAction(
  "query-runners/QUERY_RESULT_START",
)<{
  queryType: QueryTypeT;
}>();
export const queryResultReset = createAction(
  "query-runners/QUERY_RESULT_RESET",
)<{
  queryType: QueryTypeT;
}>();
export const queryResultRunning = createAction(
  "query-runners/QUERY_RESULT_RUNNING",
)<{
  queryType: QueryTypeT;
  progress?: number;
}>();

const getQueryErrorMessage = ({
  t,
  status,
  error,
}: {
  t: TFunction;
  status: "CANCELED" | "FAILED";
  error: ErrorResponseT | null;
}): string => {
  if (status === "CANCELED") {
    return t("queryRunner.queryCanceled");
  }

  return (
    (error &&
      error.code &&
      getExternalSupportedErrorMessage(t, error.code, error.context)) ||
    t("queryRunner.queryFailed")
  );
};

export const queryResultErrorAction = createAction(
  "query-runners/QUERY_RESULT_ERROR",
)<{
  queryType: QueryTypeT;
  error?: string;
}>();

const queryResultError = (
  t: TFunction,
  queryType: QueryTypeT,
  e: GetQueryErrorResponseT | Error,
) => {
  if (e instanceof Error)
    return queryResultErrorAction(errorPayload(e, { queryType }));

  return queryResultErrorAction(
    errorPayload(e, {
      error: getQueryErrorMessage({ t, status: e.status, error: e.error }),
      queryType,
    }),
  );
};

export const queryResultSuccess = createAction(
  "query-runners/QUERY_RESULT_SUCCESS",
)<{
  data: GetQueryResponseDoneT;
  queryType: QueryTypeT;
  datasetId: DatasetT["id"];
}>();

const useQueryResult = (queryType: QueryTypeT) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const getQuery = useGetQuery();
  const { loadQueries } = useLoadQueries();
  const { loadFormConfigs } = useLoadFormConfigs();

  const queryResult = (datasetId: DatasetT["id"], queryId: QueryIdT) => {
    dispatch(queryResultStart({ queryType }));

    return getQuery(datasetId, queryId).then(
      (r) => {
        // Indicate that looking for the result has stopped,
        // but not necessarily succeeded
        dispatch(queryResultReset({ queryType }));

        switch (r.status) {
          case "DONE":
            dispatch(
              queryResultSuccess(successPayload(r, { queryType, datasetId })),
            );

            // Now there should be a new result that can be queried
            loadQueries(datasetId);
            loadFormConfigs(datasetId);
            break;
          case "FAILED":
            dispatch(queryResultError(t, queryType, r));
            break;
          case "RUNNING":
            dispatch(
              queryResultRunning({
                queryType,
                progress: r.progress || undefined,
              }),
            );
            // Try again after a short time:
            //   Use the "long polling" strategy, where we assume that the
            //   backend blocks the request for a couple of seconds and waits
            //   for the query comes back.
            //   If it doesn't come back the request resolves and
            //   we - the frontend - try again almost instantly.
            setTimeout(
              () => queryResult(datasetId, queryId),
              QUERY_AGAIN_TIMEOUT,
            );
            break;
          case "NEW":
          default:
            break;
        }
      },
      (e: Error) => dispatch(queryResultError(t, queryType, e)),
    );
  };

  return queryResult;
};
