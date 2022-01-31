import { ActionType, getType } from "typesafe-actions";

import type { ConceptT, ConceptIdT, SecondaryId } from "../api/types";
import type { Action } from "../app/actions";
import { nodeIsElement } from "../model/node";

import {
  clearSearchQuery,
  clearTrees,
  loadTree,
  loadTrees,
  searchTrees,
  toggleShowMismatches,
} from "./actions";
import { setTree } from "./globalTreeStoreHelper";

export type LoadedConcept = ConceptT & {
  loading?: boolean;
  error?: string;
  success?: boolean;
};

export interface TreesT {
  [treeId: string]: LoadedConcept;
}

export interface SearchT {
  allOpen: boolean;
  showMismatches: boolean;
  loading: boolean;
  query: string | null;
  words: string[] | null;
  result: null | Record<ConceptIdT, number>;
  resultCount: number;
  duration: number;
}

export interface ConceptTreesStateT {
  loading: boolean;
  trees: TreesT;
  search: SearchT;
  secondaryIds: SecondaryId[];
  error?: string;
}

const initialSearch = {
  allOpen: false,
  showMismatches: true,
  loading: false,
  query: null,
  words: null,
  result: null,
  resultCount: 0,
  duration: 0,
};

const initialState: ConceptTreesStateT = {
  loading: false,
  trees: {},
  search: initialSearch,
  secondaryIds: [],
};

const setSearchTreesSuccess = (
  state: ConceptTreesStateT,
  { query, result }: ActionType<typeof searchTrees.success>["payload"],
): ConceptTreesStateT => {
  // only create keys array once, then cache,
  // since the result might be > 100k entries
  const resultCount = Object.keys(result).length;
  const AUTO_UNFOLD_AT = 300;

  return {
    ...state,
    search: {
      ...state.search,
      query,
      result,
      resultCount,
      allOpen: resultCount < AUTO_UNFOLD_AT,
      showMismatches: resultCount >= AUTO_UNFOLD_AT,
      loading: false,
      words: query.split(" "),
      duration: Date.now() - state.search.duration,
    },
  };
};

const setSearchTreesStart = (
  state: ConceptTreesStateT,
  { query }: ActionType<typeof searchTrees.request>["payload"],
): ConceptTreesStateT => {
  return {
    ...state,
    search: {
      ...state.search,
      loading: !!query && query.length > 0,
      query,
      words: query ? query.split(" ") : [],
      result: {},
      resultCount: 0,
      duration: Date.now(),
    },
  };
};

const updateTree = (
  state: ConceptTreesStateT,
  treeId: string,
  attributes: Partial<LoadedConcept>,
): ConceptTreesStateT => {
  return {
    ...state,
    trees: {
      ...state.trees,
      [treeId]: {
        ...state.trees[treeId],
        ...attributes,
      },
    },
  };
};

const setTreeLoading = (
  state: ConceptTreesStateT,
  { treeId }: ActionType<typeof loadTree.request>["payload"],
): ConceptTreesStateT => {
  return updateTree(state, treeId, { loading: true });
};

const setTreeSuccess = (
  state: ConceptTreesStateT,
  { data, treeId }: ActionType<typeof loadTree.success>["payload"],
): ConceptTreesStateT => {
  // Side effect in a reducer.
  // Globally store the huge (1-5 MB) trees for read only
  // - keeps the redux store free from huge data
  const newState = updateTree(state, treeId, { loading: false, success: true });

  const rootConcept = newState.trees[treeId];

  setTree(rootConcept, treeId, data);

  return newState;
};

const setTreeError = (
  state: ConceptTreesStateT,
  { treeId, message }: ActionType<typeof loadTree.failure>["payload"],
): ConceptTreesStateT => {
  return updateTree(state, treeId, {
    loading: false,
    error: message,
  });
};

const setLoadTreesSuccess = (
  state: ConceptTreesStateT,
  {
    data: { concepts, secondaryIds },
  }: ActionType<typeof loadTrees.success>["payload"],
): ConceptTreesStateT => {
  // Assign default select filter values
  for (const concept of Object.values(concepts)) {
    const tables = nodeIsElement(concept) ? concept.tables || [] : [];

    for (const table of tables) {
      for (const filter of table.filters || [])
        if (filter.defaultValue) filter.value = filter.defaultValue;
    }
  }

  return {
    ...state,
    loading: false,
    trees: concepts,
    secondaryIds,
  };
};

const conceptTrees = (
  state: ConceptTreesStateT = initialState,
  action: Action,
): ConceptTreesStateT => {
  switch (action.type) {
    // All trees
    case getType(loadTrees.request):
      return { ...state, loading: true };
    case getType(loadTrees.success):
      return setLoadTreesSuccess(state, action.payload);
    case getType(loadTrees.failure):
      return { ...state, loading: false, error: action.payload.message };

    // Individual tree:
    case getType(loadTree.request):
      return setTreeLoading(state, action.payload);
    case getType(loadTree.success):
      return setTreeSuccess(state, action.payload);
    case getType(loadTree.failure):
      return setTreeError(state, action.payload);

    case getType(clearTrees):
      return initialState;

    case getType(searchTrees.request):
      return setSearchTreesStart(state, action.payload);
    case getType(searchTrees.success):
      return setSearchTreesSuccess(state, action.payload);
    case getType(searchTrees.failure):
      return {
        ...state,
        search: { ...state.search, loading: false, duration: 0 },
        error: action.payload.message,
      };
    case getType(clearSearchQuery):
      return {
        ...state,
        search: initialSearch,
      };
    case getType(toggleShowMismatches):
      return {
        ...state,
        search: {
          ...state.search,
          allOpen: !state.search.showMismatches ? false : state.search.allOpen,
          showMismatches: !state.search.showMismatches,
        },
      };
    default:
      return state;
  }
};

export default conceptTrees;
