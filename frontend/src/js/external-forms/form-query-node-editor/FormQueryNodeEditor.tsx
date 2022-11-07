import { useEffect, useState } from "react";
import { useSelector } from "react-redux";

import type { PostPrefixForSuggestionsParams } from "../../api/api";
import {
  ConceptIdT,
  DatasetT,
  PostFilterSuggestionsResponseT,
  SelectOptionT,
  SelectorResultType,
} from "../../api/types";
import type { StateT } from "../../app/reducers";
import { toUpperCaseUnderscore } from "../../common/helpers/commonHelper";
import { usePrevious } from "../../common/helpers/usePrevious";
import type { NodeResetConfig } from "../../model/node";
import { tableIsEditable } from "../../model/table";
import QueryNodeEditor from "../../query-node-editor/QueryNodeEditor";
import type { DragItemConceptTreeNode } from "../../standard-query-editor/types";
import type { ModeT } from "../../ui-components/InputRange";
import type { EditedFormQueryNodePosition } from "../form-concept-group/FormConceptGroup";
import { initTables } from "../transformers";

interface PropsT {
  formType: string;
  fieldName: string;
  nodePosition: EditedFormQueryNodePosition;
  node: DragItemConceptTreeNode;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  allowlistedSelects?: SelectorResultType[];
  blocklistedSelects?: SelectorResultType[];
  onCloseModal: () => void;
  onUpdateLabel: (label: string) => void;
  onToggleTable: (tableIdx: number, isExcluded: boolean) => void;
  onDropConcept: (concept: DragItemConceptTreeNode) => void;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
  onSetFilterValue: (
    tableIdx: number,
    filterIdx: number,
    filterValue: any,
  ) => void;
  onSwitchFilterMode: (
    tableIdx: number,
    filterIdx: number,
    mode: ModeT,
  ) => void;
  onResetAllSettings: (config: NodeResetConfig) => void;
  onResetTable: (tableIdx: number, config: NodeResetConfig) => void;
  onSelectSelects: (selectedSelects: SelectOptionT[]) => void;
  onSelectTableSelects: (
    tableIdx: number,
    selectedSelects: SelectOptionT[],
  ) => void;
  onLoadFilterSuggestions: (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number,
    config?: { returnOnly?: boolean },
  ) => Promise<PostFilterSuggestionsResponseT | null>;
  onSetDateColumn: (tableIdx: number, dateColumnValue: string) => void;
}

const FormQueryNodeEditor = (props: PropsT) => {
  const datasetId = useSelector<StateT, DatasetT["id"] | null>(
    (state) => state.datasets.selectedDatasetId,
  );

  const [editedNode, setEditedNode] = useState(props.node);

  const previousNodePosition = usePrevious(props.nodePosition);
  useEffect(
    function () {
      if (previousNodePosition !== props.nodePosition) {
        setEditedNode(
          initTables({
            blocklistedTables: props.blocklistedTables,
            allowlistedTables: props.allowlistedTables,
          })(props.node),
        );
      }
    },
    [
      previousNodePosition,
      props.nodePosition,
      props.node,
      props.blocklistedTables,
      props.allowlistedTables,
    ],
  );

  useEffect(
    function syncWithNodeFromOutside() {
      setEditedNode(props.node);
    },
    [props.node],
  );

  const showTables =
    !!editedNode &&
    editedNode.tables &&
    editedNode.tables.length > 1 &&
    editedNode.tables.some((table) => tableIsEditable(table));

  if (!datasetId || !editedNode) {
    return null;
  }

  return (
    <QueryNodeEditor
      datasetId={datasetId}
      name={`${props.formType}_${toUpperCaseUnderscore(props.fieldName)}`}
      onLoadFilterSuggestions={props.onLoadFilterSuggestions}
      node={editedNode}
      showTables={showTables}
      blocklistedTables={props.blocklistedTables}
      allowlistedTables={props.allowlistedTables}
      blocklistedSelects={props.blocklistedSelects}
      allowlistedSelects={props.allowlistedSelects}
      onCloseModal={props.onCloseModal}
      onUpdateLabel={props.onUpdateLabel}
      onDropConcept={props.onDropConcept}
      onRemoveConcept={props.onRemoveConcept}
      onToggleTable={props.onToggleTable}
      onSelectSelects={props.onSelectSelects}
      onSelectTableSelects={props.onSelectTableSelects}
      onSetFilterValue={props.onSetFilterValue}
      onSwitchFilterMode={props.onSwitchFilterMode}
      onResetTable={props.onResetTable}
      onResetAllSettings={props.onResetAllSettings}
      onSetDateColumn={props.onSetDateColumn}
    />
  );
};

export default FormQueryNodeEditor;
