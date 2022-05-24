import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import type { ColumnDescription } from "../api/types";
import { StateT } from "../app/reducers";
import { ErrorObject, errorPayload } from "../common/actions";
import { loadCSV } from "../file/csv";

import { PreviewStateT } from "./reducer";

export type PreviewActions = ActionType<
  typeof loadCSVForPreview | typeof closePreview | typeof openPreview
>;

export const openPreview = createAction("preview/OPENk")();
export const closePreview = createAction("preview/CLOSE")();

export const loadCSVForPreview = createAsyncAction(
  "preview/LOAD_CSV_START",
  "preview/LOAD_CSV_SUCCESS",
  "preview/LOAD_CSV_ERROR",
)<
  void,
  { csv: string[][]; columns: ColumnDescription[]; resultUrl: string },
  ErrorObject
>();

export function useLoadPreviewData() {
  const dispatch = useDispatch();
  const { dataLoadedForResultUrl, data } = useSelector<StateT, PreviewStateT>(
    (state) => state.preview,
  );

  return async (url: string, columns: ColumnDescription[]) => {
    if (dataLoadedForResultUrl === url)
      return {
        csv: data.csv,
        columns: data.resultColumns,
        resultUrl: dataLoadedForResultUrl,
      };

    dispatch(loadCSVForPreview.request());

    try {
      const result = await loadCSV(url);

      const payload = {
        csv: result.data,
        columns,
        resultUrl: url,
      };

      dispatch(loadCSVForPreview.success(payload));

      return payload;
    } catch (e) {
      dispatch(loadCSVForPreview.failure(errorPayload(e as Error, {})));
    }
  };
}
