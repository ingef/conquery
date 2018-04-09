// @flow

import { type NodeType }  from '../common/types/backend';
import { isEmpty }        from '../common/helpers';

import {
  LOAD_TREES_START,
  LOAD_TREES_SUCCESS,
  LOAD_TREES_ERROR,
  LOAD_TREE_START,
  LOAD_TREE_SUCCESS,
  LOAD_TREE_ERROR,
  CLEAR_TREES,
  SEARCH_TREES,
}                         from './actionTypes';

import { setTree }        from './globalTreeStoreHelper';

export type TreesType = { [treeId: string]: NodeType }

export type SearchType = { trees: TreesType, words: Array<string>, result: TreesType }

export type StateType = {
  loading: boolean,
  version: any,
  trees: TreesType,
  search?: SearchType,
};

const initialState: StateType = {
  loading: false,
  version: null,
  trees: {},
  search: { trees: {}, words: [], result: {} }
};

const searchTrees = (state: StateType, action: Object): StateType => {
  const query = action.payload.query;
  var search = { trees: state.trees, words: query ? query.split(' ') : [], result: {} };

  if (isEmpty(query))
    return {
      ...state,
      search
    };

  const categoryTrees = window.categoryTrees;
  search.result = searching(categoryTrees, query);

return {
    ...state,
    search
  };
};

const searching = (categoryTrees: TreesType, query) => {
  return Object.assign({}, ...Object.entries(categoryTrees).map(([treeId, treeNode]) => ({
    [treeId]: findTreeNodes(treeId, treeNode, query)
  })));
}

const findTreeNodes = (treeId: string, treeNode: NodeType, query: string) => {
  const node = treeNode[treeId];
  const children = node.children || [];
  const result = children.map(child => findTreeNodes(child, treeNode, query))
    .reduce((agg, cur) => [...agg, ...cur], []);

  const label = node.label || '';
  const description = node.description || '';
  const additionalInfos = node.additionalInfos
    ? node.additionalInfos.map(t => { return t.key + " " + t.value   }).join('')
    : '';

  if ((result.length ||
      (fuzzyMatch(label, query).length > 0) ||
      (fuzzyMatch(description, query).length > 0) ||
      (fuzzyMatch(additionalInfos, query).length > 0)))
        return [treeId, ...result];

  return [];
}

const fuzzyMatch = (text: string, search: string) => {
    if (!text) return '';

    search = search.replace(/ /g, '').toLowerCase();
    var tokens = [];
    var searchPosition = 0;

    // Go through each character in the text
    for (var n = 0; n < text.length; n++) {
        var textChar = text[n];
        if (searchPosition < search.length &&
          textChar && textChar.toLowerCase() === search[searchPosition])
            searchPosition += 1;

        tokens.push(textChar);
    }
    // If are characters remaining in the search text,
    // return an empty string to indicate no match
    if (searchPosition !== search.length)
      return '';

    return tokens.join('');
}

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
  const newState = updateTree(state, action, { loading: false });

  const rootConcept = newState.trees[treeId];

  setTree(rootConcept, treeId, data);

  return newState;
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
    case SEARCH_TREES:
      return searchTrees(state, action);
    case CLEAR_TREES:
      return initialState;
    default:
      return state;
  }
};

export default categoryTrees;
