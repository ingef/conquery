// @flow

import type { Dispatch } from "redux";

import api from "../api";
import { defaultSuccess, defaultError } from "../common/actions";

import {
  MODAL_CLOSE,
  RESOLVE_FILTER_VALUES_START,
  RESOLVE_FILTER_VALUES_SUCCESS,
  RESOLVE_FILTER_VALUES_ERROR
} from "./actionTypes";

export const resolveFilterValuesStart = () => ({
  type: RESOLVE_FILTER_VALUES_START
});
export const resolveFilterValuesSuccess = (res: any, payload?: Object) =>
  defaultSuccess(RESOLVE_FILTER_VALUES_SUCCESS, res, payload);
export const resolveFilterValuesError = (err: any) =>
  defaultError(RESOLVE_FILTER_VALUES_ERROR, err);

export const resolveFilterValues = (
  datasetId,
  treeId,
  tableId,
  filterId,
  values
) => (dispatch: Dispatch<*>) => {
  dispatch(resolveFilterValuesStart());

  return api
    .postFilterValuesResolve(datasetId, treeId, tableId, filterId, values)
    .then(
      r => {
        dispatch(resolveFilterValuesSuccess(r));

        return r;
      },
      e => dispatch(resolveFilterValuesError(e))
    );
};

export const uploadFilterListModalClose = () => ({
  type: MODAL_CLOSE
});
