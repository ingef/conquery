import React from "react";
import { StateT } from "app-types";

import { createConnectedQueryNodeEditor } from "../query-node-editor/QueryNodeEditor";

import type { PropsType } from "../query-node-editor/QueryNodeEditor";

import { tableIsEditable } from "../model/table";

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
  setSelects,
  setTableSelects,
  setDateColumn,
  toggleSecondaryIdExclude,
} from "./actions";

const findNodeBeingEdited = (query) =>
  query
    .reduce((acc, group) => [...acc, ...group.elements], [])
    .find((element) => element.isEditing);

const mapStateToProps = (state: StateT) => {
  const node = findNodeBeingEdited(state.queryEditor.query);

  const showTables =
    node &&
    !!node.tables &&
    node.tables.some((table) => tableIsEditable(table));

  return {
    node,
    editorState: state.queryNodeEditor,
    showTables,
    isExcludeTimestampsPossible: true,
    isExcludeFromSecondaryIdQueryPossible: !!node && !node.isPreviousQuery,
    currencyConfig: state.startup.config.currency,
  };
};

const mapDispatchToProps = (dispatch) => ({
  onCloseModal: () => dispatch(deselectNode()),
  onUpdateLabel: (label) => dispatch(updateNodeLabel(label)),
  onDropConcept: (concept) => dispatch(addConceptToNode(concept)),
  onRemoveConcept: (conceptId) => dispatch(removeConceptFromNode(conceptId)),
  onToggleTable: (tableIdx, isExcluded) =>
    dispatch(toggleTable(tableIdx, isExcluded)),
  onSelectSelects: (value) => dispatch(setSelects(value)),
  onSelectTableSelects: (tableIdx, value) =>
    dispatch(setTableSelects(tableIdx, value)),
  onSetFilterValue: (tableIdx, filterIdx, value) =>
    dispatch(setFilterValue(tableIdx, filterIdx, value)),
  onSwitchFilterMode: (tableIdx, filterIdx, mode) =>
    dispatch(switchFilterMode(tableIdx, filterIdx, mode)),
  onResetAllFilters: (andIdx, orIdx) =>
    dispatch(resetAllFilters(andIdx, orIdx)),
  onToggleTimestamps: () => dispatch(toggleTimestamps()),
  onToggleSecondaryIdExclude: () => dispatch(toggleSecondaryIdExclude()),
  onLoadFilterSuggestions: (...params) =>
    dispatch(loadFilterSuggestions(...params)),
  onSetDateColumn: (tableIdx, value) =>
    dispatch(setDateColumn(tableIdx, value)),
});

const QueryNodeEditor = createConnectedQueryNodeEditor(
  mapStateToProps,
  mapDispatchToProps
);

export default (props: PropsType) => (
  <QueryNodeEditor name="standard" {...props} />
);
