import { useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import {
  useGetEntityHistory,
  useGetEntityHistoryDefaultParams,
} from "../api/api";
import type { ColumnDescription, DatasetT, EntityInfo } from "../api/types";
import type { StateT } from "../app/reducers";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { ErrorObject, errorPayload } from "../common/actions";
import { useIsHistoryEnabled } from "../common/feature-flags/useIsHistoryEnabled";
import { formatStdDate, getFirstAndLastDateOfRange } from "../common/helpers";
import { exists } from "../common/helpers/exists";
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

  return useCallback(
    async (datasetId: DatasetT["id"]) => {
      if (!isHistoryEnabled) return;

      try {
        const result = await getEntityHistoryDefaultParams(datasetId);

        dispatch(loadDefaultHistoryParamsSuccess({ sources: result }));
      } catch (error) {
        // TODO: Fail without noticing user, maybe change this later if required
        console.error(error);
      }
    },
    [dispatch, getEntityHistoryDefaultParams, isHistoryEnabled],
  );
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
    currentEntityInfos: EntityInfo[];
    currentEntityId: string;
    uniqueSources: string[];
    entityIds?: string[];
    label?: string;
    columns?: Record<string, ColumnDescription>;
    columnDescriptions?: ColumnDescription[];
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

  return useCallback(
    async ({
      entityId,
      entityIds,
      label,
    }: {
      entityId: string;
      entityIds?: string[];
      years?: number[];
      label?: string;
    }) => {
      if (!datasetId) return;

      try {
        dispatch(loadHistoryData.request());

        const { resultUrls, columnDescriptions, infos } =
          await getEntityHistory(
            datasetId,
            entityId,
            defaultEntityHistoryParams.sources,
          );

        const csvUrl = resultUrls.find((url) => url.endsWith("csv"));

        if (!csvUrl) {
          throw new Error("No CSV URL found");
        }

        const authorizedCSVUrl = getAuthorizedUrl(csvUrl);
        const csv = await loadCSV(authorizedCSVUrl, { english: true });
        const currentEntityData = await parseCSVWithHeaderToObj(
          csv.data.map((r) => r.join(";")).join("\n"),
        );

        const currentEntityDataProcessed =
          transformEntityData(currentEntityData);
        const uniqueSources = [
          ...new Set(currentEntityDataProcessed.map((row) => row.source)),
        ];

        const csvHeader = csv.data[0];
        const columns: Record<string, ColumnDescription> = Object.fromEntries(
          csvHeader
            .map((key) => [
              key,
              columnDescriptions.find(({ label }) => label === key),
            ])
            .filter(([, columnDescription]) => exists(columnDescription)),
        );

        dispatch(
          loadHistoryData.success({
            currentEntityCsvUrl: csvUrl,
            currentEntityData: currentEntityDataProcessed,
            currentEntityId: entityId,
            currentEntityInfos: infos,
            columnDescriptions,
            columns,
            uniqueSources,
            ...(entityIds ? { entityIds } : {}),
            ...(label ? { label } : {}),
          }),
        );
      } catch (e) {
        dispatch(loadHistoryData.failure(errorPayload(e as Error, {})));
      }
    },
    [
      datasetId,
      defaultEntityHistoryParams.sources,
      dispatch,
      getAuthorizedUrl,
      getEntityHistory,
    ],
  );
}

const transformEntityData = (data: { [key: string]: any }[]): EntityEvent[] => {
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
