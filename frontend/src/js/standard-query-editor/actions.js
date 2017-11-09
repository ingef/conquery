import { createQueryNodeModalActions } from '../query-node-modal';

import {
  DROP_AND_NODE,
  DROP_OR_NODE,
  DELETE_NODE,
  DELETE_GROUP,
  TOGGLE_EXCLUDE_GROUP,
  LOAD_QUERY,
  CLEAR_QUERY,
  EXPAND_PREVIOUS_QUERY,
} from './actionTypes';


export const dropAndNode = (item) => ({
  type: DROP_AND_NODE,
  payload: { item }
});

export const dropOrNode = (item, andIdx) => ({
  type: DROP_OR_NODE,
  payload: { item, andIdx }
});

export const deleteNode = (andIdx, orIdx) => ({
  type: DELETE_NODE,
  payload: { andIdx, orIdx }
});

export const deleteGroup = (andIdx, orIdx) => ({
  type: DELETE_GROUP,
  payload: { andIdx, orIdx }
});

export const toggleExcludeGroup = (andIdx) => ({
  type: TOGGLE_EXCLUDE_GROUP,
  payload: { andIdx }
});

export const loadQuery = (query) => ({
  type: LOAD_QUERY,
  payload: { query }
});

export const clearQuery = () => ({ type: CLEAR_QUERY });

export const expandPreviousQuery = (rootConcepts, groups) => ({
  type: EXPAND_PREVIOUS_QUERY,
  payload: { rootConcepts, groups }
});

export const {
  setStandardNode,
  clearStandardNode,
  toggleStandardTable,
  setStandardFilterValue,
  switchStandardFilterMode,
  resetStandardAllFilters,
  toggleStandardTimestamps,
  loadStandardFilterSuggestions,
} = createQueryNodeModalActions('standard');
