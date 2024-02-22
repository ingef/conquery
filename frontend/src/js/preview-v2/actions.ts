import { AsyncRecordBatchStreamReader, RecordBatch } from "apache-arrow";
import { t } from "i18next";
import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";
import { useGetQuery, useGetResult, usePreviewStatistics } from "../api/api";
import { GetQueryResponseT, PreviewStatisticsResponse } from "../api/types";
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
  queryData: GetQueryResponseT;
  arrowReader: AsyncRecordBatchStreamReader;
  initialTableData: IteratorResult<RecordBatch>;
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
  const getQuery = useGetQuery();
  const getResult = useGetResult();
  const getStatistics = usePreviewStatistics();

  const {
    dataLoadedForQueryId,
    arrowReader,
    initialTableData,
    queryData,
    statisticsData,
  } = useSelector<StateT, PreviewStateT>((state) => state.preview);
  const currentPreviewData: PreviewData | null =
    dataLoadedForQueryId &&
    arrowReader &&
    initialTableData &&
    queryData &&
    statisticsData
      ? {
          queryId: dataLoadedForQueryId,
          statisticsData,
          queryData,
          arrowReader,
          initialTableData,
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
      const arrowReader = await AsyncRecordBatchStreamReader.from(
        getResult(queryId),
      );
      const loadInitialData = async () => {
        await arrowReader.open();
        return await arrowReader.next();
      };

      // load data simultaneously
      const awaitedData = await Promise.all([
        getStatistics(queryId),
        getQuery(queryId),
        loadInitialData(),
      ]);
      const payload = {
        statisticsData: awaitedData[0],
        queryData: awaitedData[1],
        arrowReader: arrowReader,
        initialTableData: awaitedData[2],
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
