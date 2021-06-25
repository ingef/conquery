import { useDispatch } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import { usePostConceptsListToResolve } from "../api/api";
import type { ConceptIdT, PostConceptResolveResponseT } from "../api/types";
import { errorPayload, ErrorObject } from "../common/actions";
import { exists } from "../common/helpers/exists";
import { useDatasetId } from "../dataset/selectors";

export type UploadConceptListModalActions = ActionType<
  | typeof resolveConcepts
  | typeof selectConceptRootNode
  | typeof initUploadConceptListModal
  | typeof resetUploadConceptListModal
>;

export const resolveConcepts = createAsyncAction(
  "upload-concept-list-modal/RESOLVE_CONCEPTS_START",
  "upload-concept-list-modal/RESOLVE_CONCEPTS_SUCCESS",
  "upload-concept-list-modal/RESOLVE_CONCEPTS_ERROR",
)<
  undefined,
  {
    data: PostConceptResolveResponseT;
  },
  ErrorObject
>();

export const selectConceptRootNode = createAction(
  "upload-concept-list-modal/SELECT_CONCEPT_ROOT_NODE",
)<{ conceptId: ConceptIdT }>();

export const useSelectConceptRootNodeAndResolveCodes = () => {
  const dispatch = useDispatch();
  const postConceptsListToResolve = usePostConceptsListToResolve();
  const datasetId = useDatasetId();

  return (treeId: string | null, conceptCodes: string[]) => {
    if (exists(treeId)) {
      dispatch(selectConceptRootNode({ conceptId: treeId }));
    } else {
      return dispatch(selectConceptRootNode({ conceptId: "" }));
    }

    if (!datasetId) {
      return;
    }

    dispatch(resolveConcepts.request());

    return postConceptsListToResolve(datasetId, treeId, conceptCodes).then(
      (r) => dispatch(resolveConcepts.success({ data: r })),
      (e) => dispatch(resolveConcepts.failure(errorPayload(e, {}))),
    );
  };
};

export const initUploadConceptListModal = createAction(
  "upload-concept-list-modal/INIT",
)<{
  rows: string[];
  filename: string;
}>();

export const resetUploadConceptListModal = createAction(
  "upload-concept-list-modal/RESET",
)();
