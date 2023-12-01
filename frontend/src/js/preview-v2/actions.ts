import { Table, tableFromIPC } from "apache-arrow";
import { t } from "i18next";
import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";
import { useGetResult, usePreviewStatistics } from "../api/api";
import { PreviewStatisticsResponse } from "../api/types";
import { StateT } from "../app/reducers";
import { ErrorObject } from "../common/actions/genericActions";
import { setMessage } from "../snack-message/actions";
import { SnackMessageType } from "../snack-message/reducer";
import { PreviewStateT } from "./reducer";

export type PreviewActions = ActionType<
  | typeof loadPreview
  | typeof closePreview
  | typeof openPreview
  | typeof updateQueryId
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
export const updateQueryId = createAction("preview/UPDATE_LAST_QUERY_ID")<{
  queryId: string;
}>();

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
      // load data simultaneously
      const awaitedData = await Promise.all([
        getStatistics(queryId),
        tableFromIPC(getResult(queryId)),
      ]);
      const payload = {
        statisticsData: awaitedData[0],
        tableData: awaitedData[1],
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
      dispatch(loadPreview.failure({}));
      console.error(err);
    }
    return null;
  };
}
