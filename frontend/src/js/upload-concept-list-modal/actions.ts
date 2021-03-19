import { defaultSuccess, defaultError } from "../common/actions";
import { getUniqueFileRows } from "../common/helpers/fileHelper";
import type { ConceptIdT } from "../api/types";
import { useDatasetId } from "../dataset/selectors";

import {
  SELECT_CONCEPT_ROOT_NODE,
  RESOLVE_CONCEPTS_START,
  RESOLVE_CONCEPTS_SUCCESS,
  RESOLVE_CONCEPTS_ERROR,
  INIT,
  RESET,
} from "./actionTypes";
import { useDispatch } from "react-redux";
import { usePostConceptsListToResolve } from "../api/api";
import { exists } from "../common/helpers/exists";

export const resolveConceptsStart = () => ({ type: RESOLVE_CONCEPTS_START });
export const resolveConceptsSuccess = (res: any, payload?: Object) =>
  defaultSuccess(RESOLVE_CONCEPTS_SUCCESS, res, payload);
export const resolveConceptsError = (err: any) =>
  defaultError(RESOLVE_CONCEPTS_ERROR, err);

export const selectConceptRootNode = (conceptId: ConceptIdT) => ({
  type: SELECT_CONCEPT_ROOT_NODE,
  conceptId,
});

export const useSelectConceptRootNodeAndResolveCodes = () => {
  const dispatch = useDispatch();
  const postConceptsListToResolve = usePostConceptsListToResolve();
  const datasetId = useDatasetId();

  return (treeId: string | null, conceptCodes: string[]) => {
    if (exists(treeId)) {
      dispatch(selectConceptRootNode(treeId));
    } else {
      return dispatch(selectConceptRootNode(""));
    }

    if (!datasetId) {
      return;
    }

    dispatch(resolveConceptsStart());

    return postConceptsListToResolve(datasetId, treeId, conceptCodes).then(
      (r) => dispatch(resolveConceptsSuccess(r)),
      (e) => dispatch(resolveConceptsError(e))
    );
  };
};

export const initUploadConceptListModal = (file) => async (dispatch) => {
  const rows = await getUniqueFileRows(file);

  return dispatch({
    type: INIT,
    payload: { rows, filename: file.name },
  });
};

export const resetUploadConceptListModal = () => ({
  type: RESET,
});
