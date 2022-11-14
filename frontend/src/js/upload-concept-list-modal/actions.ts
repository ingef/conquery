import { useCallback } from "react";
import { useDispatch } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import { usePostConceptsListToResolve } from "../api/api";
import type { PostConceptResolveResponseT } from "../api/types";
import { errorPayload, ErrorObject } from "../common/actions/genericActions";
import { useDatasetId } from "../dataset/selectors";

export type UploadConceptListModalActions = ActionType<
  | typeof resolveConcepts
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

export const useResolveCodes = () => {
  const dispatch = useDispatch();
  const postConceptsListToResolve = usePostConceptsListToResolve();
  const datasetId = useDatasetId();

  return useCallback(
    async (treeId: string, conceptCodes: string[]) => {
      if (!datasetId) {
        return;
      }

      dispatch(resolveConcepts.request());
      try {
        const results = await postConceptsListToResolve(
          datasetId,
          treeId,
          conceptCodes,
        );
        dispatch(resolveConcepts.success({ data: results }));
      } catch (e) {
        dispatch(resolveConcepts.failure(errorPayload(e as Error, {})));
      }
    },
    [datasetId, dispatch, postConceptsListToResolve],
  );
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
