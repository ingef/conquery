import { loadCSV } from "../file/csv";

import {
  OPEN_PREVIEW,
  CLOSE_PREVIEW,
  LOAD_CSV_START,
  LOAD_CSV_ERROR,
} from "./actionTypes";

import { defaultError } from "../common/actions";
import type { ColumnDescription } from "../api/types";

export function closePreview() {
  return {
    type: CLOSE_PREVIEW,
  };
}

const loadCSVStart = () => ({ type: LOAD_CSV_START });
const loadCSVError = (err: any) => defaultError(LOAD_CSV_ERROR, err);
const loadCSVSuccess = (
  parsed: { result: { data: string[][] } },
  columns: ColumnDescription[]
) => ({
  type: OPEN_PREVIEW,
  payload: {
    csv: parsed.result.data,
    columns,
  },
});

export function openPreview(url: string, columns: ColumnDescription[]) {
  return async (dispatch) => {
    dispatch(loadCSVStart());

    try {
      const parsed = await loadCSV(url);

      dispatch(loadCSVSuccess(parsed, columns));
    } catch (e) {
      dispatch(loadCSVError(e));
    }
  };
}
