import React from "react";

import { toUpperCaseUnderscore } from "../../common/helpers";

import {
  selectReduxFormState,
  selectEditedConceptPosition,
  selectEditedConcept,
  selectSuggestions,
  selectFormContextState,
} from "../stateSelectors";
import { tableIsEditable } from "../../model/table";
import { useLoadFormFilterSuggestions } from "../form-suggestions/actions";
import { FormStateMap } from "redux-form";
import { useSelector } from "react-redux";
import { StateT } from "app-types";
import { FormContextStateT } from "../reducer";
import { ConceptIdT, CurrencyConfigT } from "../../api/types";
import QueryNodeEditor from "../../query-node-editor/QueryNodeEditor";

export type PropsType = {
  formType: string;
  fieldName: string;
  blacklistedTables?: string[];
  whitelistedTables?: string[];
  datasetId: number;
  onCloseModal: Function;
  onUpdateLabel: Function;
  onToggleTable: Function;
  onDropConcept: Function;
  onSetFilterValue: Function;
  onSwitchFilterMode: Function;
  onResetAllFilters: Function;
};

export const FormQueryNodeEditor = (props: PropsType) => {
  const reduxFormState = useSelector<StateT, FormStateMap | null>(
    selectReduxFormState
  );
  const conceptPosition = selectEditedConceptPosition(
    reduxFormState,
    props.formType,
    props.fieldName
  );

  const node = conceptPosition
    ? selectEditedConcept(
        reduxFormState,
        props.formType,
        props.fieldName,
        conceptPosition,
        props.blacklistedTables,
        props.whitelistedTables
      )
    : null;

  const { andIdx, orIdx } = conceptPosition || {};

  const showTables =
    node &&
    node.tables &&
    (node.tables.length > 1 ||
      node.tables.some((table) => tableIsEditable(table)));

  const formState = useSelector<StateT, FormContextStateT | null>((state) =>
    selectFormContextState(state, props.formType)
  );
  const suggestions =
    conceptPosition && formState
      ? selectSuggestions(formState, props.fieldName, conceptPosition)
      : null;

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

  const loadFilterSuggestions = useLoadFormFilterSuggestions();
  const onLoadFilterSuggestions = (...args: any[]) =>
    loadFilterSuggestions(
      props.formType,
      props.fieldName,
      andIdx,
      orIdx,
      ...args
    );

  return (
    <QueryNodeEditor
      name={`${props.formType}_${toUpperCaseUnderscore(props.fieldName)}`}
      onLoadFilterSuggestions={onLoadFilterSuggestions}
      node={node}
      andIdx={andIdx}
      orIdx={orIdx}
      editorState={editorState}
      showTables={showTables}
      blacklistedTables={props.blacklistedTables}
      whitelistedTables={props.whitelistedTables}
      suggestions={suggestions}
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
