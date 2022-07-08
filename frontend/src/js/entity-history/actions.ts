import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import {
  useGetEntityHistory,
  useGetEntityHistoryDefaultParams,
} from "../api/api";
import type { ColumnDescription, DatasetT } from "../api/types";
import type { StateT } from "../app/reducers";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { ErrorObject, errorPayload } from "../common/actions";
import { useIsHistoryEnabled } from "../common/feature-flags/useIsHistoryEnabled";
import { formatStdDate, getFirstAndLastDateOfRange } from "../common/helpers";
import { useDatasetId } from "../dataset/selectors";
import { loadCSV, parseCSVWithHeaderToObj } from "../file/csv";
import { useLoadPreviewData } from "../preview/actions";

import { EntityEvent } from "./reducer";

export type EntityHistoryActions = ActionType<
  | typeof openHistory
  | typeof closeHistory
  | typeof loadHistoryData
  | typeof loadDefaultHistoryParamsSuccess
  | typeof resetCurrentEntity
>;

export const openHistory = createAction("history/OPEN")();
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

export const resetCurrentEntity = createAction(
  "history/RESET_CURRENT_ENTITY",
)();

export const loadHistoryData = createAsyncAction(
  "history/LOAD_START",
  "history/LOAD_SUCCESS",
  "history/LOAD_ERROR",
)<
  void,
  {
    currentEntityCsvUrl: string;
    currentEntityData: EntityEvent[];
    currentEntityId: string;
    entityIds?: string[];
    label?: string;
    columns?: ColumnDescription[];
  },
  ErrorObject
>();

// TODO: This starts a session with the current query results,
// but there will be other ways of starting a history session
// - from a dropped file with a list of entities
// - from a previous query
export function useNewHistorySession() {
  const dispatch = useDispatch();
  const loadPreviewData = useLoadPreviewData();
  const updateHistorySession = useUpdateHistorySession();

  return async (url: string, columns: ColumnDescription[], label: string) => {
    dispatch(loadHistoryData.request());

    const result = await loadPreviewData(url, columns, { noLoading: true });

    if (!result) {
      dispatch(
        loadHistoryData.failure(new Error("Could not load preview data")),
      );
      return;
    }

    // hard-coded column index to use for entity ids (should be "PID")
    const entityIdsColumn = result.csv.map((row) => row[2]);
    const entityIds = entityIdsColumn.slice(1); // remove header;

    if (entityIds.length === 0) {
      dispatch(loadHistoryData.failure(new Error("No entity IDs found")));
      return;
    }

    updateHistorySession({
      entityId: entityIds[0],
      entityIds,
      years: [],
      columns,
      label,
    });
  };
}

export function useUpdateHistorySession() {
  const dispatch = useDispatch();
  const datasetId = useDatasetId();
  const getEntityHistory = useGetEntityHistory();
  const getAuthorizedUrl = useGetAuthorizedUrl();

  const defaultEntityHistoryParams = useSelector<StateT, { sources: string[] }>(
    (state) => state.entityHistory.defaultParams,
  );

  return async ({
    entityId,
    entityIds,
    columns,
    label,
  }: {
    entityId: string;
    entityIds?: string[];
    columns?: ColumnDescription[];
    years?: number[];
    label?: string;
  }) => {
    if (!datasetId) return;

    try {
      dispatch(loadHistoryData.request());

      const entityResult = await getEntityHistory(
        datasetId,
        entityId,
        defaultEntityHistoryParams.sources,
      );

      const csvUrl = entityResult.resultUrls.find((url) => url.endsWith("csv"));

      if (!csvUrl) {
        throw new Error("No CSV URL found");
      }

      const authorizedCSVUrl = getAuthorizedUrl(csvUrl);
      const csv = await loadCSV(authorizedCSVUrl);
      const currentEntityData = await parseCSVWithHeaderToObj(
        csv.data.map((r) => r.join(";")).join("\n"),
      );

      const currentEntityDataProcessed = transformEntityData(currentEntityData);

      dispatch(
        loadHistoryData.success({
          currentEntityCsvUrl: csvUrl,
          currentEntityData: currentEntityDataProcessed,
          currentEntityId: entityId,
          ...(entityIds ? { entityIds } : {}),
          ...(columns ? { columns } : {}),
          ...(label ? { label } : {}),
        }),
      );
    } catch (e) {
      dispatch(loadHistoryData.failure(errorPayload(e as Error, {})));
    }
  };
}

const transformEntityData = (data: { [key: string]: any }[]) => {
  return data
    .map((row) => {
      const { first, last } = getFirstAndLastDateOfRange(row["dates"]);

      return first && last
        ? {
            ...row,
            dates: {
              from: first,
              to: last,
            },
          }
        : row;
    })
    .sort((a, b) => {
      return a.dates.from - b.dates.from > 0 ? -1 : 1;
    })
    .map((row) => {
      const { dates, ...rest } = row;
      return {
        dates: {
          from: formatStdDate(row.dates?.from),
          to: formatStdDate(row.dates?.to),
        },
        ...rest,
      };
    });
};
