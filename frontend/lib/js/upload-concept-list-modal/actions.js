// @flow

import type { Dispatch } from "redux";

import api from "../api";
import { defaultSuccess, defaultError } from "../common/actions";
import { isEmpty } from "../common/helpers";
import type { TreeNodeIdType } from "../common/types/backend";

import {
  UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL,
  SELECT_CONCEPT_ROOT_NODE,
  RESOLVE_CONCEPTS_START,
  RESOLVE_CONCEPTS_SUCCESS,
  RESOLVE_CONCEPTS_ERROR,
  UPLOAD_CONCEPT_LIST_MODAL_OPEN,
  UPLOAD_CONCEPT_LIST_MODAL_CLOSE,
  UPLOAD_CONCEPT_LIST_MODAL_ACCEPT,
  RESOLVE_FILTER_VALUES_START,
  RESOLVE_FILTER_VALUES_SUCCESS,
  RESOLVE_FILTER_VALUES_ERROR
} from "./actionTypes";

export const resolveConceptsStart = () => ({ type: RESOLVE_CONCEPTS_START });
export const resolveConceptsSuccess = (res: any, payload?: Object) =>
  defaultSuccess(RESOLVE_CONCEPTS_SUCCESS, res, payload);
export const resolveConceptsError = (err: any) =>
  defaultError(RESOLVE_CONCEPTS_ERROR, err);

export const resolveFilterValuesStart = () => ({
  type: RESOLVE_FILTER_VALUES_START
});
export const resolveFilterValuesSuccess = (res: any, payload?: Object) =>
  defaultSuccess(RESOLVE_FILTER_VALUES_SUCCESS, res, payload);
export const resolveFilterValuesError = (err: any) =>
  defaultError(RESOLVE_FILTER_VALUES_ERROR, err);

export const selectConceptRootNode = (conceptId: TreeNodeIdType) => ({
  type: SELECT_CONCEPT_ROOT_NODE,
  conceptId
});

export const selectConceptRootNodeAndResolveCodes = (parameters: Object) => {
  const { treeId, datasetId, conceptCodes } = parameters;

  return (dispatch: Dispatch<*>) => {
    if (isEmpty(treeId)) {
      return dispatch(selectConceptRootNode(""));
    } else {
      dispatch(selectConceptRootNode(treeId));
    }

    dispatch(resolveConceptsStart());

    return api
      .postConceptsListToResolve(datasetId, treeId, conceptCodes)
      .then(
        r => dispatch(resolveConceptsSuccess(r)),
        e => dispatch(resolveConceptsError(e))
      );
  };
};

export const resolveConceptFilterValues = (
  datasetId,
  treeId,
  tableId,
  filterId,
  values,
  filename
) => (dispatch: Dispatch<*>) => {
  dispatch(resolveFilterValuesStart());

  return api
    .postConceptFilterValuesResolve(
      datasetId,
      treeId,
      tableId,
      filterId,
      values
    )
    .then(
      r => {
        dispatch(resolveFilterValuesSuccess(r, { filename }));

        return r;
      },
      e => dispatch(resolveFilterValuesError(e))
    );
};

export const uploadConceptListModalUpdateLabel = (label: string) => ({
  type: UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL,
  label
});

export const uploadConceptListModalOpen = (fileType: GenericFileType) => {
  const parameters = fileType.parameters;

  if (parameters.treeId)
    return fileType.callback && fileType.callback(parameters);

  return { type: UPLOAD_CONCEPT_LIST_MODAL_OPEN, payload: parameters };
};

export const uploadConceptListModalClose = () => ({
  type: UPLOAD_CONCEPT_LIST_MODAL_CLOSE
});

export const uploadConceptListModalAccept = (
  label,
  rootConcepts,
  resolutionResult
) => {
  return {
    type: UPLOAD_CONCEPT_LIST_MODAL_ACCEPT,
    payload: { label, rootConcepts, resolutionResult }
  };
};

export const acceptAndCloseUploadConceptListModal = (
  label,
  rootConcepts,
  resolutionResult
) => {
  return dispatch => {
    dispatch([
      uploadConceptListModalAccept(label, rootConcepts, resolutionResult),
      uploadConceptListModalClose()
    ]);
  };
};
