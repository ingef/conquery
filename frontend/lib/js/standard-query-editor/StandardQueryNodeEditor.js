// @flow

import React from "react";

import { createConnectedQueryNodeEditor } from "../query-node-editor";
import { hasConceptChildren } from "../concept-trees/globalTreeStoreHelper";

import type { PropsType } from "../query-node-editor/QueryNodeEditor";

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
  dropFilterValuesFile,
  toggleIncludeSubnodes,
  setSelects,
  setTableSelects
} from "./actions";

const findNodeBeingEdited = query =>
  query
    .reduce((acc, group) => [...acc, ...group.elements], [])
    .find(element => element.isEditing);

const mapStateToProps = state => {
  const node = findNodeBeingEdited(state.queryEditor.query);

  const showTables =
    node &&
    node.tables &&
    node.tables.some(table => table.filters && table.filters.length > 0);

  return {
    node,
    editorState: state.queryNodeEditor,
    showTables,
    disabledTables: [],
    isExcludeTimestampsPossible: true,
    canIncludeSubnodes: hasConceptChildren(node),
    currencyConfig: state.startup.config.currency
  };
};

const mapDispatchToProps = dispatch => ({
  onCloseModal: () => dispatch(deselectNode()),
  onUpdateLabel: label => dispatch(updateNodeLabel(label)),
  onDropConcept: concept => dispatch(addConceptToNode(concept)),
  onRemoveConcept: conceptId => dispatch(removeConceptFromNode(conceptId)),
  onToggleTable: (tableIdx, isExcluded) =>
    dispatch(toggleTable(tableIdx, isExcluded)),
  onSelectSelects: value => dispatch(setSelects(value)),
  onSelectTableSelects: (tableIdx, value) =>
    dispatch(setTableSelects(tableIdx, value)),
  onSetFilterValue: (tableIdx, filterIdx, value) =>
    dispatch(setFilterValue(tableIdx, filterIdx, value)),
  onSwitchFilterMode: (tableIdx, filterIdx, mode) =>
    dispatch(switchFilterMode(tableIdx, filterIdx, mode)),
  onResetAllFilters: (andIdx, orIdx) =>
    dispatch(resetAllFilters(andIdx, orIdx)),
  onToggleTimestamps: () => dispatch(toggleTimestamps(null, null)),
  onLoadFilterSuggestions: (...params) =>
    dispatch(loadFilterSuggestions(...params)),
  onDropFilterValuesFile: (...params) =>
    dispatch(dropFilterValuesFile(...params)),
  onToggleIncludeSubnodes: isIncludeSubNodes =>
    dispatch(toggleIncludeSubnodes(isIncludeSubNodes))
});

const QueryNodeEditor = createConnectedQueryNodeEditor(
  mapStateToProps,
  mapDispatchToProps
);

export default (props: PropsType) => (
  <QueryNodeEditor name="standard" {...props} />
);
