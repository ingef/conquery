import { loadCSV } from "../file/csv";

import {
  OPEN_PREVIEW,
  CLOSE_PREVIEW,
  LOAD_CSV_START,
  LOAD_CSV_ERROR
} from "./actionTypes";

import { defaultError } from "../common/actions";

export function closePreview() {
  return {
    type: CLOSE_PREVIEW
  };
}

const loadCSVStart = () => ({ type: LOAD_CSV_START });
const loadCSVError = (err: any) => defaultError(LOAD_CSV_ERROR, err);
const loadCSVSuccess = parsed => ({
  type: OPEN_PREVIEW,
  payload: {
    csv: parsed.result.data
  }
});

export function openPreview(url: string) {
  return async dispatch => {
    dispatch(loadCSVStart());

    try {
      const parsed = await loadCSV(url);

      dispatch(loadCSVSuccess(parsed));
    } catch (e) {
      dispatch(loadCSVError(e));
    }
  };
}
