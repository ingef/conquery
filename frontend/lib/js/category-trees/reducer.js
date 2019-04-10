// @flow

import type { NodeType, TreeNodeIdType } from "../common/types/backend";

import { includes } from "../common/helpers/commonHelper";

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
  CHANGE_SEARCH_QUERY,
  TOGGLE_ALL_OPEN
} from "./actionTypes";
import { setTree, getConceptById } from "./globalTreeStoreHelper";

export type TreesType = { [treeId: string]: NodeType };

export type SearchType = {
  allOpen: boolean,
  loading: boolean,
  query: string,
  words: ?(string[]),
  result: ?{ [TreeNodeIdType]: number },
  limit: number,
  totalResults: number,
  duration: number
};

export type StateType = {
  loading: boolean,
  version: any,
  trees: TreesType,
  search: SearchType
};

const initialSearch = {
  allOpen: false,
  loading: false,
  query: "",
  words: null,
  result: null,
  limit: 0,
  totalResults: 0,
  duration: 0
};

const initialState: StateType = {
  loading: false,
  version: null,
  trees: {},
  search: initialSearch
};

const treeWithCounts = (tree, result, searchTerm) => {
  const isNodeIncluded =
    includes(tree.label.toLowerCase(), searchTerm.toLowerCase()) ||
    (tree.description &&
      includes(tree.description.toLowerCase(), searchTerm.toLowerCase()));

  const children = tree.children
    ? tree.children.filter(key => includes(result, key))
    : [];

  if (children.length === 0) {
    return {
      [tree.id]: isNodeIncluded ? 1 : 0
    };
  } else {
    const childrenWithCounts = children.reduce((all, childKey) => {
      const child = {
        id: childKey,
        ...getConceptById(childKey)
      };
      const childResult = treeWithCounts(child, result, searchTerm);

      return {
        ...all,
        ...childResult
      };
    }, {});

    return {
      ...childrenWithCounts,
      [tree.id]: children.reduce(
        (sum, child) => sum + childrenWithCounts[child],
        0
      )
    };
  }
};

const resultWithCounts = (trees, result: string[], searchTerm) => {
  return Object.keys(trees)
    .filter(key => includes(result, key))
    .reduce((all, key) => {
      return {
        ...all,
        ...treeWithCounts({ id: key, ...trees[key] }, result, searchTerm)
      };
    }, {});
};

const setSearchTreesSuccess = (state: StateType, action: Object): StateType => {
  const {
    query,
    searchResult: { result, size, limit }
  } = action.payload;

  const nextResult = result ? resultWithCounts(state.trees, result, query) : {};

  return {
    ...state,
    search: {
      ...state.search,
      loading: false,
      query,
      words: query.split(" "),
      result: nextResult,
      limit,
      totalResults: size || 0, // The number of all potential matches (possiby greater than limit)
      duration: Date.now() - state.search.duration
    }
  };
};

const setSearchTreesStart = (state: StateType, action: Object): StateType => {
  const { query } = action.payload;

  return {
    ...state,
    search: {
      ...state.search,
      loading: query && query.length > 0,
      query: query,
      words: query ? query.split(" ") : [],
      result: {},
      totalResults: 0,
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
        search: { ...state.search, loading: false },
        error: action.payload.message
      };
    case CLEAR_SEARCH_QUERY:
      return {
        ...state,
        search: initialSearch
      };
    case CHANGE_SEARCH_QUERY:
      return {
        ...state,
        search: {
          ...state.search,
          query: action.payload.query
        }
      };

    case TOGGLE_ALL_OPEN:
      return {
        ...state,
        search: {
          ...state.search,
          allOpen: !state.search.allOpen
        }
      };
    default:
      return state;
  }
};

export default categoryTrees;
