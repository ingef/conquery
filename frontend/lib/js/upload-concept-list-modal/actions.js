// @flow

import type { Dispatch } from "redux";

import api from "../api";
import { defaultSuccess, defaultError } from "../common/actions";
import { isEmpty } from "../common/helpers";
import type { ConceptIdT } from "../api/types";

import {
  UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL,
  SELECT_CONCEPT_ROOT_NODE,
  RESOLVE_CONCEPTS_START,
  RESOLVE_CONCEPTS_SUCCESS,
  RESOLVE_CONCEPTS_ERROR,
  UPLOAD_CONCEPT_LIST_MODAL_OPEN,
  UPLOAD_CONCEPT_LIST_MODAL_CLOSE,
  UPLOAD_CONCEPT_LIST_MODAL_ACCEPT
} from "./actionTypes";

export const resolveConceptsStart = () => ({ type: RESOLVE_CONCEPTS_START });
export const resolveConceptsSuccess = (res: any, payload?: Object) =>
  defaultSuccess(RESOLVE_CONCEPTS_SUCCESS, res, payload);
export const resolveConceptsError = (err: any) =>
  defaultError(RESOLVE_CONCEPTS_ERROR, err);

export const selectConceptRootNode = (conceptId: ConceptIdT) => ({
  type: SELECT_CONCEPT_ROOT_NODE,
  conceptId
});

export const selectConceptRootNodeAndResolveCodes = (
  datasetId,
  treeId,
  conceptCodes
) => (dispatch: Dispatch<*>) => {
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

export const uploadConceptListModalUpdateLabel = (label: string) => ({
  type: UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL,
  label
});

export const uploadConceptListModalOpen = (rows, filename) => {
  return { type: UPLOAD_CONCEPT_LIST_MODAL_OPEN, payload: { rows, filename } };
};

export const uploadConceptListModalClose = () => ({
  type: UPLOAD_CONCEPT_LIST_MODAL_CLOSE
});

export const uploadConceptListModalAccept = (
  label,
  rootConcepts,
  resolvedConcepts
) => {
  return {
    type: UPLOAD_CONCEPT_LIST_MODAL_ACCEPT,
    payload: { label, rootConcepts, resolvedConcepts }
  };
};

export const acceptAndCloseUploadConceptListModal = (...params) => {
  return dispatch => {
    dispatch([
      uploadConceptListModalAccept(...params),
      uploadConceptListModalClose()
    ]);
  };
};
