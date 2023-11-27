import { Table, tableFromIPC } from "apache-arrow";
import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";
import { useGetResult, usePreviewStatistics } from "../api/api";
import { StateT } from "../app/reducers";
import { ErrorObject } from "../common/actions/genericActions";
import { PreviewStateT } from "./reducer";
import { PreviewStatisticsResponse } from "../api/types";
import { SnackMessageType } from "../snack-message/reducer";
import { t } from "i18next";
import { setMessage } from "../snack-message/actions";
import { stat } from "fs";

export type PreviewActions = ActionType<
  typeof loadPreview | typeof closePreview | typeof openPreview | typeof updateQueryId
>;

interface PreviewData {
  statisticsData: PreviewStatisticsResponse;
  tableData: Table;
  queryId: string;
}

export const loadPreview = createAsyncAction(
  "preview/LOAD_START",
  "preview/LOAD_SUCCESS",
  "preview/LOAD_ERROR",
)<void, PreviewData, ErrorObject>();

export const openPreview = createAction("preview/OPEN")();
export const closePreview = createAction("preview/CLOSE")();

// TODO: is there a better way?!?
export const updateQueryId = createAction("preview/UPDATE_LAST_QUERY_ID")<{queryId: string}>();

export function useLoadPreviewData() {
  const dispatch = useDispatch();
  const getResult = useGetResult();
  const getStatistics = usePreviewStatistics();

  const { dataLoadedForQueryId, tableData, statisticsData } = useSelector<
    StateT,
    PreviewStateT
  >((state) => state.preview);
  const currentPreviewData: PreviewData | null =
    dataLoadedForQueryId && tableData && statisticsData
      ? {
          queryId: dataLoadedForQueryId,
          statisticsData,
          tableData,
        }
      : null;

  return async (
    queryId: string,
    { noLoading }: { noLoading: boolean } = { noLoading: false },
  ): Promise<PreviewData | null> => {
    if (currentPreviewData && dataLoadedForQueryId === queryId) {
      return currentPreviewData;
    }

    if (!noLoading) {
      dispatch(loadPreview.request());
    }

    try {
      const payload = {
        statisticsData: await getStatistics(queryId),
        tableData: await tableFromIPC(getResult(queryId)),
        queryId,
      };
      dispatch(loadPreview.success(payload));
      return payload;
    } catch (err) {
      dispatch(
        setMessage({
          message: t("preview.loadingError"),
          type: SnackMessageType.ERROR,
        }),
      );
      console.error(err);
    }
    return null;
  };
}
