import { StateT } from "app-types";
import React from "react";
import { useSelector } from "react-redux";
import { FormStateMap } from "redux-form";

import type { PostPrefixForSuggestionsParams } from "../../api/api";
import {
  ConceptIdT,
  CurrencyConfigT,
  DatasetIdT,
  SelectOptionT,
} from "../../api/types";
import { toUpperCaseUnderscore } from "../../common/helpers";
import { tableIsEditable } from "../../model/table";
import QueryNodeEditor from "../../query-node-editor/QueryNodeEditor";
import { DragItemNode } from "../../standard-query-editor/types";
import { ModeT } from "../../ui-components/InputRange";
import { FormContextStateT } from "../reducer";
import {
  selectReduxFormState,
  selectEditedConceptPosition,
  selectEditedConcept,
  selectFormContextState,
} from "../stateSelectors";

interface PropsT {
  formType: string;
  fieldName: string;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  onCloseModal: (andIdx: number, orIdx: number) => void;
  onUpdateLabel: (andIdx: number, orIdx: number, label: string) => void;
  onToggleTable: (
    valueIdx: number,
    conceptIdx: number,
    tableIdx: number,
    isExcluded: boolean,
  ) => void;
  onDropConcept: (
    valueIdx: number,
    conceptIdx: number,
    concept: DragItemNode,
  ) => void;
  onRemoveConcept: (
    valueIdx: number,
    conceptIdx: number,
    conceptId: ConceptIdT,
  ) => void;
  onSetFilterValue: (
    valueIdx: number,
    conceptIdx: number,
    tableIdx: number,
    filterIdx: number,
    filterValue: any,
  ) => void;
  onSwitchFilterMode: (
    valueIdx: number,
    conceptIdx: number,
    tableIdx: number,
    filterIdx: number,
    mode: ModeT,
  ) => void;
  onResetAllFilters: (valueIdx: number, conceptIdx: number) => void;
  onSelectSelects: (
    valueIdx: number,
    conceptIdx: number,
    selectedSelects: SelectOptionT[],
  ) => void;
  onSelectTableSelects: (
    valueIdx: number,
    conceptIdx: number,
    tableIdx: number,
    selectedSelects: SelectOptionT[],
  ) => void;
  onLoadFilterSuggestions: (
    andIdx: number,
    orIdx: number,
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number,
  ) => void;
  onSetDateColumn: (
    valueIdx: number,
    conceptIdx: number,
    tableIdx: number,
    dateColumnValue: string | null,
  ) => void;
}

const FormQueryNodeEditor = (props: PropsT) => {
  const reduxFormState = useSelector<StateT, FormStateMap | null>(
    selectReduxFormState,
  );
  const conceptPosition = selectEditedConceptPosition(
    reduxFormState,
    props.formType,
    props.fieldName,
  );

  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );

  const node = conceptPosition
    ? selectEditedConcept(
        reduxFormState,
        props.formType,
        props.fieldName,
        conceptPosition,
        props.blocklistedTables,
        props.allowlistedTables,
      )
    : null;

  const { andIdx, orIdx } = conceptPosition || {};

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

  if (!datasetId || !node || !editorState) {
    return null;
  }

  return (
    <QueryNodeEditor
      datasetId={datasetId}
      name={`${props.formType}_${toUpperCaseUnderscore(props.fieldName)}`}
      onLoadFilterSuggestions={(...args) =>
        props.onLoadFilterSuggestions(andIdx, orIdx, ...args)
      }
      node={node}
      editorState={editorState}
      showTables={showTables}
      blocklistedTables={props.blocklistedTables}
      allowlistedTables={props.allowlistedTables}
      currencyConfig={currencyConfig}
      onCloseModal={() => props.onCloseModal(andIdx, orIdx)}
      onUpdateLabel={(label) => props.onUpdateLabel(andIdx, orIdx, label)}
      onDropConcept={(node) => props.onDropConcept(andIdx, orIdx, node)}
      onRemoveConcept={(conceptId) =>
        props.onRemoveConcept(andIdx, orIdx, conceptId)
      }
      onToggleTable={(...args) => props.onToggleTable(andIdx, orIdx, ...args)}
      onSelectSelects={(value) => {
        props.onSelectSelects(andIdx, orIdx, value);
      }}
      onSelectTableSelects={(...args) =>
        props.onSelectTableSelects(andIdx, orIdx, ...args)
      }
      onSetFilterValue={(...args) =>
        props.onSetFilterValue(andIdx, orIdx, ...args)
      }
      onSwitchFilterMode={(...args) =>
        props.onSwitchFilterMode(andIdx, orIdx, ...args)
      }
      onResetAllFilters={() => props.onResetAllFilters(andIdx, orIdx)}
      onSetDateColumn={(...args) =>
        props.onSetDateColumn(andIdx, orIdx, ...args)
      }
    />
  );
};

export default FormQueryNodeEditor;
