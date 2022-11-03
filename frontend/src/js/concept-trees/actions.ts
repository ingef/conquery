import { useCallback } from "react";
import { useDispatch } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import { useGetConcepts, useGetConcept } from "../api/api";
import type {
  DatasetT,
  ConceptIdT,
  GetConceptsResponseT,
  GetConceptResponseT,
} from "../api/types";
import { ErrorObject, successPayload, errorPayload } from "../common/actions";
import { isEmpty } from "../common/helpers";
import { Sema } from "../common/helpers/rateLimitHelper";
import { getDatasetId } from "../dataset/globalDatasetHelper";

import { resetAllTrees, globalSearch } from "./globalTreeStoreHelper";
import type { TreesT } from "./reducer";

export type ConceptTreeActions = ActionType<
  | typeof clearTrees
  | typeof loadTrees
  | typeof loadTree
  | typeof clearSearchQuery
  | typeof toggleShowMismatches
  | typeof searchTrees
>;

export const clearTrees = createAction("concept-trees/CLEAR_TREES")();

export const loadTrees = createAsyncAction(
  "concept-trees/LOAD_TREES_START",
  "concept-trees/LOAD_TREES_SUCCESS",
  "concept-trees/LOAD_TREES_ERROR",
)<void, { data: GetConceptsResponseT }, ErrorObject>();

export const useLoadTrees = () => {
  const dispatch = useDispatch();
  const getConcepts = useGetConcepts();
  const loadTree = useLoadTree();

  return useCallback(
    async (datasetId: DatasetT["id"]) => {
      // CAREFUL: side effect!
      resetAllTrees();

      dispatch(clearTrees());
      dispatch(loadTrees.request());

      try {
        const result = await getConcepts(datasetId);

        dispatch(loadTrees.success(successPayload(result, {})));

        if (!result.concepts) return;

        for (const treeId of Object.keys(result.concepts)) {
          if (result.concepts[treeId].detailsAvailable) {
            loadTree(datasetId, treeId);
          }
        }
      } catch (e) {
        dispatch(loadTrees.failure(errorPayload(e as Error, {})));
      }
    },
    [dispatch, getConcepts, loadTree],
  );
};

export const loadTree = createAsyncAction(
  "concept-trees/LOAD_TREE_START",
  "concept-trees/LOAD_TREE_SUCCESS",
  "concept-trees/LOAD_TREE_ERROR",
)<
  { treeId: ConceptIdT },
  { data: GetConceptResponseT; treeId: ConceptIdT },
  ErrorObject & { treeId: ConceptIdT }
>();

const TREES_TO_LOAD_IN_PARALLEL = 5;

const semaphore = new Sema(TREES_TO_LOAD_IN_PARALLEL);

export const useLoadTree = () => {
  const dispatch = useDispatch();
  const getConcept = useGetConcept();

  return useCallback(
    async (datasetId: DatasetT["id"], treeId: ConceptIdT) => {
      await semaphore.acquire();

      // If the datasetId changed in the mean time, don't load the tree
      if (datasetId !== getDatasetId()) {
        console.log(`${datasetId} not matching, not loading ${treeId}`);
        semaphore.release();
        return;
      }

      dispatch(loadTree.request({ treeId }));

      try {
        const result = await getConcept(datasetId, treeId);

        semaphore.release();
        dispatch(loadTree.success(successPayload(result, { treeId })));
      } catch (e) {
        semaphore.release();
        dispatch(loadTree.failure(errorPayload(e as Error, { treeId })));
      }
    },
    [dispatch, getConcept],
  );
};

export const searchTrees = createAsyncAction(
  "concept-trees/SEARCH_TREES_START",
  "concept-trees/SEARCH_TREES_SUCCESS",
  "concept-trees/SEARCH_TREES_ERROR",
)<
  { query: string },
  { query: string; result: Record<ConceptIdT, number> },
  ErrorObject
>();

export const useSearchTrees = () => {
  const dispatch = useDispatch();

  return useCallback(
    async (trees: TreesT, query: string) => {
      dispatch(searchTrees.request({ query }));

      if (isEmpty(query)) return;

      try {
        const result = await globalSearch(trees, query);

        dispatch(searchTrees.success({ query, result }));
      } catch (e) {
        dispatch(searchTrees.failure(errorPayload(e as Error, { query })));
      }
    },
    [dispatch],
  );
};

export const clearSearchQuery = createAction(
  "concept-trees/CLEAR_SEARCH_QUERY",
)();

export const toggleShowMismatches = createAction(
  "concept-trees/TOGGLE_SHOW_MISMATCHES",
)();
