import type { Dispatch } from "redux";

import api from "../api";
import { defaultSuccess, defaultError } from "../common/actions";
import { isEmpty } from "../common/helpers/commonHelper";
import { getUniqueFileRows } from "../common/helpers/fileHelper";
import type { ConceptIdT } from "../api/types";

import {
  SELECT_CONCEPT_ROOT_NODE,
  RESOLVE_CONCEPTS_START,
  RESOLVE_CONCEPTS_SUCCESS,
  RESOLVE_CONCEPTS_ERROR,
  INIT,
  RESET
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
) => (dispatch: Dispatch) => {
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

export const initUploadConceptListModal = file => async dispatch => {
  const rows = await getUniqueFileRows(file);

  return dispatch({
    type: INIT,
    payload: { rows, filename: file.name }
  });
};

export const resetUploadConceptListModal = () => ({
  type: RESET
});
