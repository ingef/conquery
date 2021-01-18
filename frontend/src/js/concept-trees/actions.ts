import { Sema } from "../common/helpers/rateLimitHelper";

import type { DatasetIdT, ConceptIdT } from "../api/types";
import type { TreesT } from "./reducer";

import api from "../api";
import { defaultSuccess, defaultError } from "../common/actions";
import { isEmpty } from "../common/helpers";

import { resetAllTrees, globalSearch } from "./globalTreeStoreHelper";
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

export const clearTrees = () => ({ type: CLEAR_TREES });

export const loadTreesStart = () => ({ type: LOAD_TREES_START });
export const loadTreesError = (err: any) => defaultError(LOAD_TREES_ERROR, err);
export const loadTreesSuccess = (res: any) =>
  defaultSuccess(LOAD_TREES_SUCCESS, res);

export const loadTrees = (datasetId: DatasetIdT) => {
  return async (dispatch: Dispatch) => {
    // CAREFUL: side effect!
    resetAllTrees();

    dispatch(clearTrees());
    dispatch(loadTreesStart());

    try {
      const result = await api.getConcepts(datasetId);

      dispatch(loadTreesSuccess(result));

      if (!result.concepts) return;

      for (const treeId of Object.keys(result.concepts)) {
        if (result.concepts[treeId].detailsAvailable) {
          dispatch(loadTree(datasetId, treeId));
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

export const loadTree = (datasetId: DatasetIdT, treeId: ConceptIdT) => {
  return async (dispatch: DispatchProp) => {
    await semaphore.acquire();

    // If the datasetId changed in the mean time, don't load the tree
    if (datasetId !== getDatasetId()) {
      console.log(`${datasetId} not matching, not loading ${treeId}`);
      semaphore.release();
      return;
    }

    dispatch(loadTreeStart(treeId));

    try {
      const result = await api.getConcept(datasetId, treeId);

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

export const searchTrees = (
  datasetId: DatasetIdT,
  trees: TreesT,
  query: string
) => {
  return async (dispatch: Dispatch) => {
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
