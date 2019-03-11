// @flow

import type { NodeType, TreeNodeIdType } from "../common/types/backend";

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
  CHANGE_SEARCH_QUERY
} from "./actionTypes";
import { setTree } from "./globalTreeStoreHelper";

export type TreesType = { [treeId: string]: NodeType };

export type SearchType = {
  searching: boolean,
  loading: boolean,
  query: string,
  words: Array<string>,
  result: Array<TreeNodeIdType>,
  limit: number,
  resultCount: number,
  duration: number
};

export type StateType = {
  loading: boolean,
  version: any,
  trees: TreesType,
  search?: SearchType
};

const initialState: StateType = {
  loading: false,
  version: null,
  trees: {},
  search: {
    searching: false,
    loading: false,
    query: "",
    words: [],
    result: [],
    limit: 0,
    totalResults: 0,
    duration: 0
  }
};

const setSearchTreesSuccess = (state: StateType, action: Object): StateType => {
  const {
    query,
    searchResult: { result, size, limit }
  } = action.payload;

  const searching = query && query.length > 0;

  return {
    ...state,
    search: {
      searching,
      loading: false,
      query,
      words: query ? query.split(" ") : [],
      result: result || [],
      limit,
      totalResults: searching ? size : 0,
      duration: Date.now() - state.search.duration
    }
  };
};

const setSearchTreesStart = (state: StateType, action: Object): StateType => {
  const { query } = action.payload;

  return {
    ...state,
    search: {
      searching: false,
      loading: query && query.length > 0,
      query: query,
      words: query ? query.split(" ") : [],
      result: [],
      resultCount: 0,
      limit: 0,
      duration: Date.now()
    }
  };
};

const updateTree = (
  state: StateType,
  action: Object,
  attributes: Object
): StateType => {
  return {
    ...state,
    trees: {
      ...state.trees,
      [action.payload.treeId]: {
        ...state.trees[action.payload.treeId],
        ...attributes
      }
    }
  };
};

const setTreeLoading = (state: StateType, action: Object): StateType => {
  return updateTree(state, action, { loading: true });
};

const setTreeSuccess = (state: StateType, action: Object): StateType => {
  // Side effect in a reducer.
  // Globally store the huge (1-5 MB) trees for read only
  // - keeps the redux store free from huge data
  const { treeId, data } = action.payload;
  const newState = updateTree(state, action, { loading: false });

  const rootConcept = newState.trees[treeId];

  setTree(rootConcept, treeId, data);

  return newState;
};

const setTreeError = (state: StateType, action: Object): StateType => {
  return updateTree(state, action, {
    loading: false,
    error: action.payload.message
  });
};

const setLoadTreesSuccess = (state: StateType, action: Object): StateType => {
  return {
    ...state,
    loading: false,
    version: action.payload.data.version,
    trees: action.payload.data.concepts
  };
};

const categoryTrees = (
  state: StateType = initialState,
  action: Object
): StateType => {
  switch (action.type) {
    // All trees
    case LOAD_TREES_START:
      return { ...state, loading: true };
    case LOAD_TREES_SUCCESS:
      return setLoadTreesSuccess(state, action);
    case LOAD_TREES_ERROR:
      return { ...state, loading: false, error: action.payload.message };

    // Individual tree:
    case LOAD_TREE_START:
      return setTreeLoading(state, action);
    case LOAD_TREE_SUCCESS:
      return setTreeSuccess(state, action);
    case LOAD_TREE_ERROR:
      return setTreeError(state, action);
    case CLEAR_TREES:
      return initialState;
    case SEARCH_TREES_START:
      return setSearchTreesStart(state, action);
    case SEARCH_TREES_SUCCESS:
      return setSearchTreesSuccess(state, action);
    case SEARCH_TREES_ERROR:
      return {
        ...state,
        search: { loading: false },
        error: action.payload.message
      };
    case CLEAR_SEARCH_QUERY:
      return {
        ...state,
        search: { searching: false, query: "" }
      };
    case CHANGE_SEARCH_QUERY:
      return {
        ...state,
        search: {
          ...state.search,
          query: action.payload.query
        }
      };
    default:
      return state;
  }
};

export default categoryTrees;
