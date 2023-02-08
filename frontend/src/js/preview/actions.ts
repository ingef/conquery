import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import type { ColumnDescription, ResultUrlsWithLabel } from "../api/types";
import { StateT } from "../app/reducers";
import { ErrorObject, errorPayload } from "../common/actions/genericActions";
import { loadCSV } from "../file/csv";

import { PreviewStateT } from "./reducer";

export type PreviewActions = ActionType<
  typeof loadCSVForPreview | typeof closePreview | typeof openPreview
>;

export const openPreview = createAction("preview/OPENk")();
export const closePreview = createAction("preview/CLOSE")();

interface PreviewData {
  csv: string[][];
  columns: ColumnDescription[];
  resultUrl: ResultUrlsWithLabel;
}

export const loadCSVForPreview = createAsyncAction(
  "preview/LOAD_CSV_START",
  "preview/LOAD_CSV_SUCCESS",
  "preview/LOAD_CSV_ERROR",
)<void, PreviewData, ErrorObject>();

export function useLoadPreviewData() {
  const dispatch = useDispatch();
  const { dataLoadedForResultUrl, data } = useSelector<StateT, PreviewStateT>(
    (state) => state.preview,
  );
  const currentPreviewData: PreviewData | null =
    data.csv && data.resultColumns && dataLoadedForResultUrl
      ? {
          csv: data.csv,
          columns: data.resultColumns,
          resultUrl: dataLoadedForResultUrl,
        }
      : null;

  return async (
    url: ResultUrlsWithLabel,
    columns: ColumnDescription[],
    { noLoading }: { noLoading: boolean } = { noLoading: false },
  ): Promise<PreviewData | null> => {
    if (currentPreviewData && dataLoadedForResultUrl === url) {
      return currentPreviewData;
    }

    if (!noLoading) {
      dispatch(loadCSVForPreview.request());
    }

    try {
      const result = await loadCSV(url.url);
      const payload = {
        csv: result.data,
        columns,
        resultUrl: url,
      };

      dispatch(loadCSVForPreview.success(payload));

      return payload;
    } catch (e) {
      dispatch(loadCSVForPreview.failure(errorPayload(e as Error, {})));

      return null;
    }
  };
}
