// @flow

import React                              from 'react';

import { createConnectedQueryNodeEditor } from '../query-node-editor';

import {
  deselectNode,
  updateNodeLabel,
  addConceptToNode,
  removeConceptFromNode,
  toggleTable,
  setFilterValue,
  switchFilterMode,
  resetAllFilters,
  toggleTimestamps,
  loadFilterSuggestions,
  onDropFiles,
} from './actions';

const findNodeBeingEdited = (query) =>
  query
    .reduce((acc, group) => [...acc, ...group.elements], [])
    .find(element => element.isEditing);

const mapStateToProps = (state) => {
  const node = findNodeBeingEdited(state.panes.right.tabs.queryEditor.query);

  const showTables = node && node.tables && (
    node.tables.length > 1 ||
    node.tables.some(table => table.filters && table.filters.length > 0)
  );

  return {
    node,
    editorState: state.queryNodeEditor,
    showTables,
    isExcludeTimestampsPossible: true,
    currencyConfig: state.startup.config.currency
  };
}

const mapDispatchToProps = (dispatch) => ({
  onCloseModal: () => dispatch(deselectNode()),
  onUpdateLabel: (label) => dispatch(updateNodeLabel(label)),
  onDropConcept: (concept) => dispatch(addConceptToNode(concept)),
  onRemoveConcept: (conceptId) => dispatch(removeConceptFromNode(conceptId)),
  onToggleTable: (tableIdx, isExcluded) =>
    dispatch(toggleTable(
      tableIdx,
      isExcluded
    )),
  onSetFilterValue: (tableIdx, filterIdx, value, formattedValue) =>
    dispatch(setFilterValue(
      tableIdx,
      filterIdx,
      value,
      formattedValue
    )),
  onSwitchFilterMode: (tableIdx, filterIdx, mode) =>
    dispatch(switchFilterMode(
      tableIdx,
      filterIdx,
      mode
    )),
  onResetAllFilters: (andIdx, orIdx) =>
    dispatch(resetAllFilters(andIdx, orIdx)),
  onToggleTimestamps: (isExcluded) =>
    dispatch(toggleTimestamps(isExcluded)),
  onLoadFilterSuggestions:
    (datasetId, tableIdx, tableId, conceptId, filterIdx, filterId, prefix) =>
      dispatch(loadFilterSuggestions(
        datasetId,
        tableIdx,
        tableId,
        conceptId,
        filterIdx,
        filterId,
        prefix
    )),
  onDropFiles: (...params) => dispatch(onDropFiles(...params))
});

const QueryNodeEditor = createConnectedQueryNodeEditor(mapStateToProps, mapDispatchToProps);

export default (props) => <QueryNodeEditor type="standard" {...props} />;
