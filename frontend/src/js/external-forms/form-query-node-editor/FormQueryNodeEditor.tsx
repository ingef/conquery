import type { PostPrefixForSuggestionsParams } from "../../api/api";
import {
  ConceptIdT,
  PostFilterSuggestionsResponseT,
  SelectOptionT,
  SelectorResultType,
} from "../../api/types";
import { toUpperCaseUnderscore } from "../../common/helpers/commonHelper";
import type { NodeResetConfig } from "../../model/node";
import { tableIsEditable } from "../../model/table";
import QueryNodeEditor from "../../query-node-editor/QueryNodeEditor";
import type { DragItemConceptTreeNode } from "../../standard-query-editor/types";
import type { ModeT } from "../../ui-components/InputRange";
import type { EditedFormQueryNodePosition } from "../form-concept-group/FormConceptGroup";

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
    filterValue: unknown,
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
  const showTables =
    !!props.node &&
    props.node.tables &&
    props.node.tables.length > 1 &&
    props.node.tables.some((table) => tableIsEditable(table));

  if (!props.node) {
    return null;
  }

  return (
    <QueryNodeEditor
      name={`${props.formType}_${toUpperCaseUnderscore(props.fieldName)}`}
      onLoadFilterSuggestions={props.onLoadFilterSuggestions}
      node={props.node}
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
