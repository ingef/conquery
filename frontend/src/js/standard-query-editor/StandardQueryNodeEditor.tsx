import { useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";

import type { ConceptIdT } from "../api/types";
import type { StateT } from "../app/reducers";
import { nodeIsConceptQueryNode } from "../model/node";
import { tableIsEditable } from "../model/table";
import QueryNodeEditor from "../query-node-editor/QueryNodeEditor";

import {
  updateNodeLabel,
  addConceptToNode,
  removeConceptFromNode,
  toggleTable,
  setFilterValue,
  switchFilterMode,
  resetAllSettings,
  resetTable,
  toggleTimestamps,
  setSelects,
  setTableSelects,
  setDateColumn,
  toggleSecondaryIdExclude,
  useLoadFilterSuggestions,
  updateNodeDescription,
} from "./actions";
import { StandardQueryNodeT } from "./types";

interface EditedNodePosition {
  andIdx: number;
  orIdx: number;
}

interface Props {
  editedNode: EditedNodePosition;
  onClose: () => void;
}

const StandardQueryNodeEditor = ({ editedNode, onClose }: Props) => {
  const node = useSelector<StateT, StandardQueryNodeT | null>(
    (state) =>
      state.queryEditor.query[editedNode.andIdx]?.elements[editedNode.orIdx],
  );
  const showTables =
    !!node &&
    nodeIsConceptQueryNode(node) &&
    !!node.tables &&
    node.tables.length > 1 &&
    node.tables.some((table) => tableIsEditable(table));

  const onLoadFilterSuggestions = useLoadFilterSuggestions(editedNode);
  const dispatch = useDispatch();
  const { andIdx, orIdx } = editedNode;

  const onUpdateLabel = useCallback(
    (label: string) => dispatch(updateNodeLabel({ andIdx, orIdx, label })),
    [dispatch, andIdx, orIdx],
  );

  const onUpdateDescription = useCallback(
    (description: string) =>
      dispatch(updateNodeDescription({ andIdx, orIdx, description })),
    [dispatch, andIdx, orIdx],
  );

  const onDropConcept = useCallback(
    (concept) => dispatch(addConceptToNode({ andIdx, orIdx, concept })),
    [dispatch, andIdx, orIdx],
  );

  const onRemoveConcept = useCallback(
    (conceptId: ConceptIdT) =>
      dispatch(removeConceptFromNode({ andIdx, orIdx, conceptId })),
    [dispatch, andIdx, orIdx],
  );

  const onToggleTable = useCallback(
    (tableIdx, isExcluded) =>
      dispatch(toggleTable({ andIdx, orIdx, tableIdx, isExcluded })),
    [dispatch, andIdx, orIdx],
  );

  const onSelectSelects = useCallback(
    (value) => {
      dispatch(setSelects({ andIdx, orIdx, value }));
    },
    [dispatch, andIdx, orIdx],
  );

  const onSelectTableSelects = useCallback(
    (tableIdx, value) =>
      dispatch(setTableSelects({ andIdx, orIdx, tableIdx, value })),
    [dispatch, andIdx, orIdx],
  );

  const onSetFilterValue = useCallback(
    (tableIdx, filterIdx, value) =>
      dispatch(setFilterValue({ andIdx, orIdx, tableIdx, filterIdx, value })),
    [dispatch, andIdx, orIdx],
  );

  const onSwitchFilterMode = useCallback(
    (tableIdx, filterIdx, mode) =>
      dispatch(switchFilterMode({ andIdx, orIdx, tableIdx, filterIdx, mode })),
    [dispatch, andIdx, orIdx],
  );

  const onResetAllSettings = useCallback(
    (config) => dispatch(resetAllSettings({ andIdx, orIdx, config })),
    [dispatch, andIdx, orIdx],
  );

  const onResetTable = useCallback(
    (tableIdx, config) =>
      dispatch(resetTable({ andIdx, orIdx, tableIdx, config })),
    [dispatch, andIdx, orIdx],
  );

  const onToggleTimeStamps = useCallback(
    () => dispatch(toggleTimestamps({ andIdx, orIdx })),
    [dispatch, andIdx, orIdx],
  );

  const onToggleSecondaryIdExclude = useCallback(
    () => dispatch(toggleSecondaryIdExclude({ andIdx, orIdx })),
    [dispatch, andIdx, orIdx],
  );

  const onSetDateColumn = useCallback(
    (tableIdx, value) =>
      dispatch(setDateColumn({ andIdx, orIdx, tableIdx, value })),
    [dispatch, andIdx, orIdx],
  );

  if (!node) {
    return null;
  }

  return (
    <QueryNodeEditor
      name="standard"
      node={node}
      showTables={showTables}
      onLoadFilterSuggestions={onLoadFilterSuggestions}
      onCloseModal={onClose}
      onUpdateLabel={onUpdateLabel}
      onUpdateDescription={onUpdateDescription}
      onDropConcept={onDropConcept}
      onRemoveConcept={onRemoveConcept}
      onToggleTable={onToggleTable}
      onSelectSelects={onSelectSelects}
      onSelectTableSelects={onSelectTableSelects}
      onSetFilterValue={onSetFilterValue}
      onSwitchFilterMode={onSwitchFilterMode}
      onResetAllSettings={onResetAllSettings}
      onResetTable={onResetTable}
      onToggleTimestamps={onToggleTimeStamps}
      onToggleSecondaryIdExclude={onToggleSecondaryIdExclude}
      onSetDateColumn={onSetDateColumn}
    />
  );
};

export default StandardQueryNodeEditor;
