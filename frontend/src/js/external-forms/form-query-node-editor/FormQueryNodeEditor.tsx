import { StateT } from "app-types";
import { useSelector } from "react-redux";

import type { PostPrefixForSuggestionsParams } from "../../api/api";
import {
  ConceptIdT,
  CurrencyConfigT,
  DatasetIdT,
  SelectOptionT,
  SelectorResultType,
} from "../../api/types";
import { toUpperCaseUnderscore } from "../../common/helpers";
import { tableIsEditable } from "../../model/table";
import QueryNodeEditor from "../../query-node-editor/QueryNodeEditor";
import type { DragItemConceptTreeNode } from "../../standard-query-editor/types";
import type { ModeT } from "../../ui-components/InputRange";
import type { FormQueryNodeEditorState } from "../form-concept-group/FormConceptGroup";
import { FormContextStateT } from "../reducer";
import { selectFormContextState } from "../stateSelectors";

interface PropsT {
  formType: string;
  fieldName: string;
  editorState: FormQueryNodeEditorState;
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
  onResetAllFilters: () => void;
  onResetTable: (tableIdx: number) => void;
  onSelectSelects: (selectedSelects: SelectOptionT[]) => void;
  onSelectTableSelects: (
    tableIdx: number,
    selectedSelects: SelectOptionT[],
  ) => void;
  onLoadFilterSuggestions: (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number,
  ) => void;
  onSetDateColumn: (tableIdx: number, dateColumnValue: string | null) => void;
}

const FormQueryNodeEditor = (props: PropsT) => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );

  const { node } = props.editorState;

  const showTables =
    !!node &&
    node.tables &&
    node.tables.length > 1 &&
    node.tables.some((table) => tableIsEditable(table));

  const formState = useSelector<StateT, FormContextStateT | null>((state) =>
    selectFormContextState(state, props.formType),
  );

  const currencyConfig = useSelector<StateT, CurrencyConfigT>(
    (state) => state.startup.config.currency,
  );
  const editorState = formState ? formState[props.fieldName] : null;

  if (!datasetId || !editorState) {
    return null;
  }

  return (
    <QueryNodeEditor
      datasetId={datasetId}
      name={`${props.formType}_${toUpperCaseUnderscore(props.fieldName)}`}
      onLoadFilterSuggestions={props.onLoadFilterSuggestions}
      node={node}
      editorState={editorState}
      showTables={showTables}
      blocklistedTables={props.blocklistedTables}
      allowlistedTables={props.allowlistedTables}
      blocklistedSelects={props.blocklistedSelects}
      allowlistedSelects={props.allowlistedSelects}
      currencyConfig={currencyConfig}
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
      onResetAllFilters={props.onResetAllFilters}
      onSetDateColumn={props.onSetDateColumn}
    />
  );
};

export default FormQueryNodeEditor;
