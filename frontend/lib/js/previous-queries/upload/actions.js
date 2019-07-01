// @flow

import T from "i18n-react";
import { type Dispatch } from "redux-thunk";

import { type DatasetIdType } from "../../dataset/reducer";

import { defaultSuccess, defaultError } from "../../common/actions";

import api from "../../api";

import { loadPreviousQueries } from "../list/actions";

import {
  OPEN_UPLOAD_MODAL,
  CLOSE_UPLOAD_MODAL,
  UPLOAD_START,
  UPLOAD_SUCCESS,
  UPLOAD_ERROR
} from "./actionTypes";

export const openUploadModal = () => ({
  type: OPEN_UPLOAD_MODAL
});

export const closeUploadModal = () => ({
  type: CLOSE_UPLOAD_MODAL
});

export const uploadStart = () => ({ type: UPLOAD_START });
export const uploadSuccess = (success: any) =>
  defaultSuccess(UPLOAD_SUCCESS, success);
export const uploadError = (error: any) => defaultError(UPLOAD_ERROR, error);

export const upload = (datasetId: DatasetIdType, query: Object) => async (
  dispatch: Dispatch
) => {
  dispatch(uploadStart());

  try {
    const results = await api.postQueries(datasetId, query, "external");

    dispatch(uploadSuccess(results));

    return dispatch(loadPreviousQueries(datasetId));
  } catch (e) {
    return dispatch(uploadError(e));
  }
};
