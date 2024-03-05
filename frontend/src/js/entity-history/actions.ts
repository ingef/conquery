import startOfYear from "date-fns/startOfYear";
import subYears from "date-fns/subYears";
import { useCallback, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import {
  useGetEntityHistory,
  useGetEntityHistoryDefaultParams,
} from "../api/api";
import type {
  ColumnDescription,
  DatasetT,
  EntityInfo,
  GetEntityHistoryDefaultParamsResponse,
  ResultUrlWithLabel,
  TimeStratifiedInfo,
} from "../api/types";
import type { StateT } from "../app/reducers";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { ErrorObject, errorPayload } from "../common/actions/genericActions";
import {
  formatStdDate,
  getFirstAndLastDateOfRange,
} from "../common/helpers/dateHelper";
import { exists } from "../common/helpers/exists";
import { useDatasetId } from "../dataset/selectors";
import { loadCSV, parseCSVWithHeaderToObj } from "../file/csv";
import { useLoadPreviewData } from "../preview/actions";
import { setMessage } from "../snack-message/actions";
import { SnackMessageType } from "../snack-message/reducer";

import { Table } from "apache-arrow";
import { EntityEvent, EntityId } from "./reducer";
import { isDateColumn, isSourceColumn } from "./timeline/util";

export type EntityHistoryActions = ActionType<
  | typeof openHistory
  | typeof closeHistory
  | typeof loadHistoryData
  | typeof loadDefaultHistoryParamsSuccess
  | typeof resetCurrentEntity
  | typeof resetHistory
>;

export const openHistory = createAction("history/OPEN")();
export const closeHistory = createAction("history/CLOSE")();

export const loadDefaultHistoryParamsSuccess = createAction(
  "history/LOAD_DEFAULT_HISTORY_PARAMS_SUCCESS",
)<GetEntityHistoryDefaultParamsResponse>();

export const useLoadDefaultHistoryParams = () => {
  const dispatch = useDispatch();
  const getEntityHistoryDefaultParams = useGetEntityHistoryDefaultParams();

  return useCallback(
    async (datasetId: DatasetT["id"]) => {
      try {
        const result = await getEntityHistoryDefaultParams(datasetId);

        dispatch(loadDefaultHistoryParamsSuccess(result));
      } catch (error) {
        // TODO: Fail without noticing user, maybe change this later if required
        console.error(error);
      }
    },
    [dispatch, getEntityHistoryDefaultParams],
  );
};

export const resetCurrentEntity = createAction(
  "history/RESET_CURRENT_ENTITY",
)();
export const resetHistory = createAction("history/RESET")();

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
    currentEntityTimeStratifiedInfos: TimeStratifiedInfo[];
    currentEntityId: EntityId;
    currentEntityUniqueSources: string[];
    resultUrls?: ResultUrlWithLabel[];
    entityIds?: EntityId[];
    label?: string;
    columns?: Record<string, ColumnDescription>;
    columnDescriptions?: ColumnDescription[];
  },
  ErrorObject
>();

// HARD-CODED values that make sense with our particular data.
// TODO: Make this configurable / get a preferred id kind list from backend
export const PREFERRED_ID_KINDS = ["EGK", "PID"];
export const DEFAULT_ID_KIND = "EGK";

// TODO: This starts a session with the current query results,
// but there will be other ways of starting a history session
// - from a dropped file with a list of entities
// - from a previous query
export function useNewHistorySession() {
  const dispatch = useDispatch();
  const loadPreviewData = useLoadPreviewData();
  const queryId = useSelector<StateT, string | null>(
    (state) => state.preview.lastQuery,
  );
  const { updateHistorySession } = useUpdateHistorySession();

  return async (label: string) => {
    if (!queryId) {
      dispatch(loadHistoryData.failure(new Error("Could not load query data")));
      return;
    }

    dispatch(loadHistoryData.request());

    const result = await loadPreviewData(queryId, {
      noLoading: true,
    });

    if (!result) {
      dispatch(
        loadHistoryData.failure(new Error("Could not load preview data")),
      );
      return;
    }

    const entityIds = new Table(result.initialTableData.value)
      .toArray()
      .map((row) => {
        for (const [k, v] of Object.entries(row)) {
          if (PREFERRED_ID_KINDS.includes(v as string)) {
            return {
              id: v as string,
              kind: k,
            };
          }
        }
      })
      .filter(exists);

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

const SHOW_LOADING_DELAY = 300;

export function useUpdateHistorySession() {
  const dispatch = useDispatch();
  const datasetId = useDatasetId();
  const getEntityHistory = useGetEntityHistory();
  const getAuthorizedUrl = useGetAuthorizedUrl();
  const { t } = useTranslation();

  const loadingIdTimeout = useRef<NodeJS.Timeout>();
  const [loadingId, setLoadingId] = useState<string>();

  const defaultEntityHistoryParams = useSelector<
    StateT,
    StateT["entityHistory"]["defaultParams"]
  >((state) => state.entityHistory.defaultParams);
  const observationPeriodMin = useSelector<StateT, string>((state) => {
    return (
      state.startup.config.observationPeriodStart ||
      formatStdDate(subYears(startOfYear(new Date()), 1))
    );
  });

  const updateHistorySession = useCallback(
    async ({
      entityId,
      entityIds,
      label,
    }: {
      entityId: EntityId;
      entityIds?: EntityId[];
      years?: number[];
      label?: string;
    }) => {
      if (!datasetId) return;

      if (loadingIdTimeout.current) {
        clearTimeout(loadingIdTimeout.current);
      }
      loadingIdTimeout.current = setTimeout(() => {
        setLoadingId(entityId.id);
      }, SHOW_LOADING_DELAY);

      try {
        dispatch(loadHistoryData.request());

        const { resultUrls, columnDescriptions, infos, timeStratifiedInfos } =
          await getEntityHistory(
            datasetId,
            entityId,
            defaultEntityHistoryParams.sources,
            {
              min: observationPeriodMin,
              max: formatStdDate(new Date()),
            },
          );

        const csvUrl = resultUrls.find(({ url }) => url.endsWith("csv"));

        if (!csvUrl) {
          throw new Error("No CSV URL found");
        }

        const authorizedCSVUrl = getAuthorizedUrl(csvUrl.url);
        const csv = await loadCSV(authorizedCSVUrl);
        const currentEntityData = await parseCSVWithHeaderToObj(
          csv.data.map((r) => r.join(";")).join("\n"),
        );
        const dateColumn = columnDescriptions.find(isDateColumn);
        if (!dateColumn) {
          throw new Error("No date column found");
        }
        const sourceColumn = columnDescriptions.find(isSourceColumn);
        if (!sourceColumn) {
          throw new Error("No sources column found");
        }

        const currentEntityDataProcessed = transformEntityData(
          currentEntityData,
          { dateColumn },
        );

        const uniqueSources = [
          ...new Set(
            currentEntityDataProcessed.map((row) => row[sourceColumn.label]),
          ),
        ] as string[];

        const csvHeader = csv.data[0];
        const columns: Record<string, ColumnDescription> = Object.fromEntries(
          csvHeader
            .map((key) => [
              key,
              columnDescriptions.find(({ label }) => label === key),
            ])
            .filter(([, columnDescription]) => exists(columnDescription)),
        );

        const nonEmptyInfos = infos.filter((i) => exists(i.value) && !!i.value);

        dispatch(
          loadHistoryData.success({
            currentEntityCsvUrl: csvUrl.url,
            currentEntityData: currentEntityDataProcessed,
            currentEntityId: entityId,
            currentEntityInfos: nonEmptyInfos,
            currentEntityTimeStratifiedInfos: timeStratifiedInfos,
            currentEntityUniqueSources: uniqueSources,
            columnDescriptions,
            resultUrls,
            columns,
            ...(entityIds ? { entityIds } : {}),
            ...(label ? { label } : {}),
          }),
        );
      } catch (e) {
        dispatch(loadHistoryData.failure(errorPayload(e as Error, {})));
        dispatch(
          setMessage({
            message: t("history.error"),
            type: SnackMessageType.ERROR,
          }),
        );
      }

      if (loadingIdTimeout.current) {
        clearTimeout(loadingIdTimeout.current);
      }
      setLoadingId(undefined);
    },
    [
      t,
      datasetId,
      defaultEntityHistoryParams,
      dispatch,
      getAuthorizedUrl,
      getEntityHistory,
      observationPeriodMin,
    ],
  );

  return {
    loadingId,
    updateHistorySession,
  };
}

interface DateRow {
  from: Date;
  to: Date;
}
const transformEntityData = (
  data: { [key: string]: unknown }[],
  {
    dateColumn,
  }: {
    dateColumn: ColumnDescription;
  },
): EntityEvent[] => {
  const dateKey = dateColumn.label;

  return data
    .map((row) => {
      const { first, last } = getFirstAndLastDateOfRange(
        row[dateKey] as string,
      );

      return first && last
        ? {
            ...row,
            [dateKey]: {
              from: first,
              to: last,
            },
          }
        : row;
    })
    .sort((a, b) => {
      return (a[dateKey] as DateRow).from.getTime() -
        (b[dateKey] as DateRow).from.getTime() >
        0
        ? -1
        : 1;
    })
    .map((row) => {
      return {
        ...row,
        [dateKey]: {
          from: formatStdDate((row[dateKey] as DateRow)?.from),
          to: formatStdDate((row[dateKey] as DateRow)?.to),
        },
      };
    });
};
