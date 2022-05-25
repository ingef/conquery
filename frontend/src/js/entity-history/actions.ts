import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import {
  useGetEntityHistory,
  useGetEntityHistoryDefaultParams,
} from "../api/api";
import { ColumnDescription } from "../api/types";
import { StateT } from "../app/reducers";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { ErrorObject, errorPayload } from "../common/actions";
import { useIsHistoryEnabled } from "../common/feature-flags/useIsHistoryEnabled";
import { DatasetT } from "../dataset/reducer";
import { useDatasetId } from "../dataset/selectors";
import { loadCSV } from "../file/csv";
import { useLoadPreviewData } from "../preview/actions";

export type EntityHistoryActions = ActionType<
  | typeof openHistory
  | typeof closeHistory
  | typeof initHistoryData
  | typeof loadDefaultHistoryParamsSuccess
>;

export const openHistory = createAction("history/CLOSE")();
export const closeHistory = createAction("history/CLOSE")();

export const loadDefaultHistoryParamsSuccess = createAction(
  "history/LOAD_DEFAULT_HISTORY_PARAMS_SUCCESS",
)<{ sources: string[] }>();

export const useLoadDefaultHistoryParams = () => {
  const dispatch = useDispatch();
  const getEntityHistoryDefaultParams = useGetEntityHistoryDefaultParams();
  const isHistoryEnabled = useIsHistoryEnabled();

  return async (datasetId: DatasetT["id"]) => {
    if (!isHistoryEnabled) return;

    try {
      const result = await getEntityHistoryDefaultParams(datasetId);

      dispatch(loadDefaultHistoryParamsSuccess({ sources: result }));
    } catch (error) {
      // TODO: Fail without noticing user, maybe change this later if required
      console.error(error);
    }
  };
};

export const initHistoryData = createAsyncAction(
  "history/INIT_START",
  "history/INIT_SUCCESS",
  "history/INIT_ERROR",
)<
  void,
  {
    entityIds: string[];
    currentEntityData: string[][];
    currentEntityId: string;
  },
  ErrorObject
>();

// TODO: This starts a session with the current query results,
// but there will be other ways of starting a history session
// - from a dropped file with a list of entities
// - from a previous query
export function useInitHistorySession() {
  const dispatch = useDispatch();
  const datasetId = useDatasetId();
  const loadPreviewData = useLoadPreviewData();
  const getEntityHistory = useGetEntityHistory();
  const getAuthorizedUrl = useGetAuthorizedUrl();

  const defaultEntityHistoryParams = useSelector<StateT, { sources: string[] }>(
    (state) => state.entityHistory.defaultParams,
  );

  let csv = useSelector<StateT, string[][] | null>(
    (state) => state.preview.data.csv,
  );

  return async (url: string, columns: ColumnDescription[]) => {
    if (!datasetId) return;

    dispatch(initHistoryData.request());

    if (!csv) {
      const result = await loadPreviewData(url, columns);

      if (!result) {
        return;
      }

      csv = result.csv;
    }

    const entityIds = csv.map((row) => row[0]);

    if (entityIds.length === 0) {
      return;
    }

    try {
      const firstEntityResult = await getEntityHistory(
        datasetId,
        entityIds[0],
        defaultEntityHistoryParams.sources,
      );

      // Assuming the first resultURL is a CSV url
      const csvUrl = firstEntityResult.resultUrls.find((url) =>
        url.endsWith("csv"),
      );

      if (!csvUrl) {
        throw new Error("No CSV URL found");
      }

      const authorizedCSVUrl = getAuthorizedUrl(csvUrl);
      const csv = await loadCSV(authorizedCSVUrl);

      dispatch(
        initHistoryData.success({
          entityIds,
          currentEntityData: csv.data,
          currentEntityId: entityIds[0],
        }),
      );
    } catch (e) {
      dispatch(initHistoryData.failure(errorPayload(e as Error, {})));
    }
  };
}
