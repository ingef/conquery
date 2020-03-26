import React from "react";

import { createConnectedQueryNodeEditor } from "../../query-node-editor/QueryNodeEditor";
import { toUpperCaseUnderscore } from "../../common/helpers";

import {
  selectReduxFormState,
  selectEditedConceptPosition,
  selectEditedConcept,
  selectSuggestions,
  selectFormContextState
} from "../stateSelectors";
import { createFormSuggestionActions } from "../form-suggestions/actions";
import { tableIsEditable } from "../../model/table";

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

const mapStateToProps = (state, ownProps) => {
  const reduxFormState = selectReduxFormState(state);
  const conceptPosition = selectEditedConceptPosition(
    reduxFormState,
    ownProps.formType,
    ownProps.fieldName
  );

  const node = conceptPosition
    ? selectEditedConcept(
        reduxFormState,
        ownProps.formType,
        ownProps.fieldName,
        conceptPosition,
        ownProps.blacklistedTables,
        ownProps.whitelistedTables
      )
    : null;

  const { andIdx, orIdx } = conceptPosition || {};

  const showTables =
    node &&
    node.tables &&
    (node.tables.length > 1 ||
      node.tables.some(table => tableIsEditable(table)));

  const formState = selectFormContextState(state, ownProps.formType);
  const suggestions = conceptPosition
    ? selectSuggestions(formState, ownProps.fieldName, conceptPosition)
    : null;

  return {
    node,
    andIdx,
    orIdx,
    editorState: formState[ownProps.fieldName],
    isExcludeTimestampsPossible: false,
    showTables,
    blacklistedTables: ownProps.blacklistedTables,
    whitelistedTables: ownProps.whitelistedTables,
    suggestions,
    currencyConfig: state.startup.config.currency,

    onToggleTimestamps: () => {},
    onCloseModal: () => ownProps.onCloseModal(andIdx, orIdx),
    onUpdateLabel: label => ownProps.onUpdateLabel(andIdx, orIdx, label),
    onDropConcept: concept => ownProps.onDropConcept(andIdx, orIdx, concept),
    onRemoveConcept: conceptId =>
      ownProps.onRemoveConcept(andIdx, orIdx, conceptId),
    onToggleTable: (...args) => ownProps.onToggleTable(andIdx, orIdx, ...args),
    onSelectSelects: (...args) =>
      ownProps.onSelectSelects(andIdx, orIdx, ...args),
    onSelectTableSelects: (...args) =>
      ownProps.onSelectTableSelects(andIdx, orIdx, ...args),
    onSetFilterValue: (...args) =>
      ownProps.onSetFilterValue(andIdx, orIdx, ...args),
    onSwitchFilterMode: (...args) =>
      ownProps.onSwitchFilterMode(andIdx, orIdx, ...args),
    onResetAllFilters: () => ownProps.onResetAllFilters(andIdx, orIdx),
    onSetDateColumn: (...args) =>
      ownProps.onSetDateColumn(andIdx, orIdx, ...args)
  };
};

const mapDispatchToProps = (dispatch, ownProps) => {
  const { loadFormFilterSuggestions } = createFormSuggestionActions(
    ownProps.formType,
    ownProps.fieldName
  );

  return {
    onLoadFilterSuggestions: (...params) => {
      return dispatch(
        loadFormFilterSuggestions(
          ownProps.formType,
          ownProps.fieldName,
          ...params
        )
      );
    }
  };
};

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...ownProps,
  ...stateProps,
  ...dispatchProps,
  onLoadFilterSuggestions: (...params) =>
    dispatchProps.onLoadFilterSuggestions(
      ...params,
      stateProps.andIdx,
      stateProps.orIdx
    )
});

const QueryNodeEditor = createConnectedQueryNodeEditor(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
);

export const FormQueryNodeEditor = (props: PropsType) => (
  <QueryNodeEditor
    name={`${props.formType}_${toUpperCaseUnderscore(props.fieldName)}`}
    {...props}
  />
);
