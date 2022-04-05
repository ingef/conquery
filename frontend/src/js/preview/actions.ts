import { useDispatch } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import type { ColumnDescription } from "../api/types";
import { ErrorObject, errorPayload } from "../common/actions";
import { loadCSV } from "../file/csv";

export type PreviewActions = ActionType<
  typeof loadCSVForPreview | typeof closePreview
>;

export const closePreview = createAction("preview/CLOSE")();

export const loadCSVForPreview = createAsyncAction(
  "preview/LOAD_CSV_START",
  "preview/LOAD_CSV_SUCCESS",
  "preview/LOAD_CSV_ERROR",
)<void, { csv: string[][]; columns: ColumnDescription[] }, ErrorObject>();

export function useOpenPreview() {
  const dispatch = useDispatch();

  return async (url: string, columns: ColumnDescription[]) => {
    dispatch(loadCSVForPreview.request());

    try {
      const result = await loadCSV(url);

      dispatch(loadCSVForPreview.success({ csv: result.data, columns }));
    } catch (e) {
      dispatch(loadCSVForPreview.failure(errorPayload(e as Error, {})));
    }
  };
}
