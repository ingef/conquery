// @flow

import {
  type TableType,
} from '../standard-query-editor/types'

import {
  type AdditionalInfosType,
} from '../tooltip';

import {
  LOAD_TREES_START,
  LOAD_TREES_SUCCESS,
  LOAD_TREES_ERROR,
  LOAD_TREE_START,
  LOAD_TREE_SUCCESS,
  LOAD_TREE_ERROR,
  CLEAR_TREES,
} from './actionTypes';
import {
  setTree
} from './globalTreeStoreHelper';

export type TreeNodeIdType = string;

export type TreeNodeType = {
  id: TreeNodeIdType,
  label: string,
  description?: string,
  loading?: boolean,
  error?: string,
  parent?: TreeNodeIdType,
  active?: boolean,
  children?: [TreeNodeIdType],
  tables?: TableType[],
  additionalInfos?: AdditionalInfosType,
  matchingEntries?: number,
};

export type TreesType = { [treeId: string]: TreeNodeType }

export type StateType = {
  loading: boolean,
  version: any,
  trees: TreesType
};

const initialState: StateType = {
  loading: false,
  version: null,
  trees: {}
};

const updateTree = (state: StateType, action: Object, attributes: Object): StateType => {
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
  const rootConcept = state.trees[treeId]

  setTree(rootConcept, treeId, data);

  return updateTree(state, action, { loading: false });
};

const setTreeError = (state: StateType, action: Object): StateType => {
  return updateTree(state, action, { loading: false, error: action.payload.message });
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
      return {
        ...state,
        loading: false,
        version: action.payload.data.version,
        trees: action.payload.data.concepts
      };
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
    default:
      return state;
  }
};

export default categoryTrees;
