// QUERY NODE MODAL

export const createActionTypes = (type: string) => ({
  SET_NODE: `query-node-modal/SET_${type}_NODE`,
  CLEAR_NODE: `query-node-modal/CLEAR_${type}_NODE`,
  TOGGLE_TABLE: `query-node-modal/TOGGLE_${type}_TABLE`,
  SET_FILTER_VALUE: `query-node-modal/SET_${type}_FILTER_VALUE`,
  RESET_ALL_FILTERS: `query-node-modal/RESET_${type}_ALL_FILTERS`,
  SWITCH_FILTER_MODE: `query-node-modal/SWITCH_${type}_FILTER_MODE`,
  TOGGLE_TIMESTAMPS: `query-node-modal/TOGGLE_${type}_TIMESTAMPS`,
  LOAD_FILTER_SUGGESTIONS_START: `query-node-modal/LOAD_${type}_FILTER_SUGGESTIONS_START`,
  LOAD_FILTER_SUGGESTIONS_SUCCESS: `query-node-modal/LOAD_${type}_FILTER_SUGGESTIONS_SUCCESS`,
  LOAD_FILTER_SUGGESTIONS_ERROR: `query-node-modal/LOAD_${type}_FILTER_SUGGESTIONS_ERROR`,
});
