import { useDispatch } from "react-redux";

import { useGetConcepts, useGetConcept } from "../api/api";
import type { DatasetIdT, ConceptIdT } from "../api/types";
import { defaultSuccess, defaultError } from "../common/actions";
import { isEmpty } from "../common/helpers";
import { Sema } from "../common/helpers/rateLimitHelper";
import { getDatasetId } from "../dataset/globalDatasetHelper";

import {
  LOAD_TREES_START,
  LOAD_TREES_SUCCESS,
  LOAD_TREES_ERROR,
  LOAD_TREE_START,
  LOAD_TREE_SUCCESS,
  LOAD_TREE_ERROR,
  CLEAR_TREES,
  SEARCH_TREES_START,
  SEARCH_TREES_SUCCESS,
  SEARCH_TREES_ERROR,
  CLEAR_SEARCH_QUERY,
  TOGGLE_SHOW_MISMATCHES,
} from "./actionTypes";
import { resetAllTrees, globalSearch } from "./globalTreeStoreHelper";
import type { TreesT } from "./reducer";

export const clearTrees = () => ({ type: CLEAR_TREES });

export const loadTreesStart = () => ({ type: LOAD_TREES_START });
export const loadTreesError = (err: any) => defaultError(LOAD_TREES_ERROR, err);
export const loadTreesSuccess = (res: any) =>
  defaultSuccess(LOAD_TREES_SUCCESS, res);

export const useLoadTrees = () => {
  const dispatch = useDispatch();
  const getConcepts = useGetConcepts();
  const loadTree = useLoadTree();

  return async (datasetId: DatasetIdT) => {
    // CAREFUL: side effect!
    resetAllTrees();

    dispatch(clearTrees());
    dispatch(loadTreesStart());

    try {
      const result = await getConcepts(datasetId);

      dispatch(loadTreesSuccess(result));

      if (!result.concepts) return;

      for (const treeId of Object.keys(result.concepts)) {
        if (result.concepts[treeId].detailsAvailable) {
          loadTree(datasetId, treeId);
        }
      }
    } catch (e) {
      dispatch(loadTreesError(e));
    }
  };
};

export const loadTreeStart = (treeId: ConceptIdT) => ({
  type: LOAD_TREE_START,
  payload: { treeId },
});
export const loadTreeError = (treeId: ConceptIdT, err: any) =>
  defaultError(LOAD_TREE_ERROR, err, { treeId });
export const loadTreeSuccess = (treeId: ConceptIdT, res: any) =>
  defaultSuccess(LOAD_TREE_SUCCESS, res, { treeId });

const TREES_TO_LOAD_IN_PARALLEL = 5;

const semaphore = new Sema(TREES_TO_LOAD_IN_PARALLEL);

export const useLoadTree = () => {
  const dispatch = useDispatch();
  const getConcept = useGetConcept();

  return async (datasetId: DatasetIdT, treeId: ConceptIdT) => {
    await semaphore.acquire();

    // If the datasetId changed in the mean time, don't load the tree
    if (datasetId !== getDatasetId()) {
      console.log(`${datasetId} not matching, not loading ${treeId}`);
      semaphore.release();
      return;
    }

    dispatch(loadTreeStart(treeId));

    try {
      const result = await getConcept(datasetId, treeId);

      semaphore.release();
      dispatch(loadTreeSuccess(treeId, result));
    } catch (e) {
      semaphore.release();
      dispatch(loadTreeError(treeId, e));
    }
  };
};

export const searchTreesStart = (query: string) => ({
  type: SEARCH_TREES_START,
  payload: { query },
});
export const searchTreesSuccess = (query: string, result: Object) => ({
  type: SEARCH_TREES_SUCCESS,
  payload: { query, result },
});
export const searchTreesError = (query: string, err: any) =>
  defaultError(SEARCH_TREES_ERROR, err, { query });

export const useSearchTrees = () => {
  const dispatch = useDispatch();

  return async (trees: TreesT, query: string) => {
    dispatch(searchTreesStart(query));

    if (isEmpty(query)) return;

    try {
      const result = await globalSearch(trees, query);

      dispatch(searchTreesSuccess(query, result));
    } catch (e) {
      dispatch(searchTreesError(query, e));
    }
  };
};

export const clearSearchQuery = () => ({ type: CLEAR_SEARCH_QUERY });

export const toggleShowMismatches = () => ({ type: TOGGLE_SHOW_MISMATCHES });
