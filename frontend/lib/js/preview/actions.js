// @flow

import { loadCSV } from "../file/csv";

import { OPEN_PREVIEW, CLOSE_PREVIEW } from "./actionTypes";

export function closePreview(url: string) {
  return {
    type: CLOSE_PREVIEW
  };
}

export function openPreview(url: string) {
  return async dispatch => {
    const parsed = await loadCSV(url);

    dispatch({
      type: OPEN_PREVIEW,
      payload: {
        csv: parsed.result.data
      }
    });
  };
}
