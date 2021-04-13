import React from "react";

import { toUpperCaseUnderscore } from "../../common/helpers";

import {
  selectReduxFormState,
  selectEditedConceptPosition,
  selectEditedConcept,
  selectFormContextState,
} from "../stateSelectors";
import { tableIsEditable } from "../../model/table";
import { FormStateMap } from "redux-form";
import { useSelector } from "react-redux";
import { StateT } from "app-types";
import { FormContextStateT } from "../reducer";
import { ConceptIdT, CurrencyConfigT, DatasetIdT } from "../../api/types";
import QueryNodeEditor from "../../query-node-editor/QueryNodeEditor";
import type { PostPrefixForSuggestionsParams } from "../../api/api";

interface PropsT {
  formType: string;
  fieldName: string;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  onCloseModal: (andIdx: number, orIdx: number) => void;
  onUpdateLabel: (andIdx: number, orIdx: number, label: string) => void;
  onToggleTable: Function;
  onDropConcept: Function;
  onSetFilterValue: Function;
  onSwitchFilterMode: Function;
  onResetAllFilters: Function;
  onLoadFilterSuggestions: (
    andIdx: number,
    orIdx: number,
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number
  ) => void;
}

const FormQueryNodeEditor = (props: PropsT) => {
  const reduxFormState = useSelector<StateT, FormStateMap | null>(
    selectReduxFormState
  );
  const conceptPosition = selectEditedConceptPosition(
    reduxFormState,
    props.formType,
    props.fieldName
  );

  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId
  );

  const node = conceptPosition
    ? selectEditedConcept(
        reduxFormState,
        props.formType,
        props.fieldName,
        conceptPosition,
        props.blocklistedTables,
        props.allowlistedTables
      )
    : null;

  const { andIdx, orIdx } = conceptPosition || {};

  const showTables =
    !!node &&
    node.tables &&
    (node.tables.length > 1 ||
      node.tables.some((table) => tableIsEditable(table)));

  const formState = useSelector<StateT, FormContextStateT | null>((state) =>
    selectFormContextState(state, props.formType)
  );

  const currencyConfig = useSelector<StateT, CurrencyConfigT>(
    (state) => state.startup.config.currency
  );
  const editorState = formState ? formState[props.fieldName] : null;

  const onCloseModal = () => props.onCloseModal(andIdx, orIdx);
  const onUpdateLabel = (label: string) =>
    props.onUpdateLabel(andIdx, orIdx, label);
  const onDropConcept = (concept) =>
    props.onDropConcept(andIdx, orIdx, concept);
  const onRemoveConcept = (conceptId: ConceptIdT) =>
    props.onRemoveConcept(andIdx, orIdx, conceptId);
  const onToggleTable = (...args) =>
    props.onToggleTable(andIdx, orIdx, ...args);
  const onSelectSelects = (...args) =>
    props.onSelectSelects(andIdx, orIdx, ...args);
  const onSelectTableSelects = (...args) =>
    props.onSelectTableSelects(andIdx, orIdx, ...args);
  const onSetFilterValue = (...args) =>
    props.onSetFilterValue(andIdx, orIdx, ...args);
  const onSwitchFilterMode = (...args) =>
    props.onSwitchFilterMode(andIdx, orIdx, ...args);
  const onResetAllFilters = () => props.onResetAllFilters(andIdx, orIdx);
  const onSetDateColumn = (...args) =>
    props.onSetDateColumn(andIdx, orIdx, ...args);
  const onLoadFilterSuggestions = (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number
  ) =>
    props.onLoadFilterSuggestions(andIdx, orIdx, params, tableIdx, filterIdx);

  if (!datasetId || !node || !editorState) {
    return null;
  }

  return (
    <QueryNodeEditor
      datasetId={datasetId}
      name={`${props.formType}_${toUpperCaseUnderscore(props.fieldName)}`}
      onLoadFilterSuggestions={onLoadFilterSuggestions}
      node={node}
      editorState={editorState}
      showTables={showTables}
      blocklistedTables={props.blocklistedTables}
      allowlistedTables={props.allowlistedTables}
      currencyConfig={currencyConfig}
      isExcludeTimestampsPossible={false}
      isExcludeFromSecondaryIdQueryPossible={false}
      onToggleTimestamps={() => {}}
      onCloseModal={onCloseModal}
      onUpdateLabel={onUpdateLabel}
      onDropConcept={onDropConcept}
      onRemoveConcept={onRemoveConcept}
      onToggleTable={onToggleTable}
      onSelectSelects={onSelectSelects}
      onSelectTableSelects={onSelectTableSelects}
      onSetFilterValue={onSetFilterValue}
      onSwitchFilterMode={onSwitchFilterMode}
      onResetAllFilters={onResetAllFilters}
      onSetDateColumn={onSetDateColumn}
    />
  );
};

export default FormQueryNodeEditor;
