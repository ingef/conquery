// @flow

import { createActionTypes }          from './actionTypes';

export const createQueryNodeEditorActions = (type: string): Object => {
  const {
    SET_DETAILS_VIEW_ACTIVE,
    SET_INPUT_TABLE_VIEW_ACTIVE,
    SET_FOCUSED_INPUT,
    TOGGLE_EDIT_LABEL,
    RESET,
    TOGGLE_INCLUDE_SUBNODES
  } = createActionTypes(type);

  const setDetailsViewActive = () => ({type: SET_DETAILS_VIEW_ACTIVE});
  const setInputTableViewActive = (tableIdx) => ({type: SET_INPUT_TABLE_VIEW_ACTIVE, tableIdx});
  const setFocusedInput = (filterIdx) => ({type: SET_FOCUSED_INPUT, filterIdx});
  const toggleEditLabel = () => ({type: TOGGLE_EDIT_LABEL});
  const reset = () => ({type: RESET});
  const onDropFiles = (datasetId, treeId, tableIdx, tableId, filterIdx, filterId, files) =>
    dropFiles(files, {
      parameters: {
        actionType: SET_RESOLVED_FILTER_VALUES,
        datasetId,
        treeId,
        tableIdx,
        tableId,
        filterIdx,
        filterId
      },
      callback: conceptFilterValuesResolve
    });
  const toggleIncludeSubnodes = () => ({type: TOGGLE_INCLUDE_SUBNODES});

  return {
    setDetailsViewActive,
    setInputTableViewActive,
    setFocusedInput,
    toggleEditLabel,
    reset,
    onDropFiles,
    toggleIncludeSubnodes
  };
};
