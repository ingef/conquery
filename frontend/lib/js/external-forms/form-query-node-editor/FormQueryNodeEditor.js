// @flow

import React                               from 'react';

import { createConnectedQueryNodeEditor }  from '../../query-node-editor';

import {
  selectFormState,
  selectReduxFormState,
  selectEditedConceptPosition,
  selectEditedConcept,
  selectSuggestions,
}                                          from '../../external-forms/stateSelectors';

import { toUpperCaseUnderscore }           from '../../common/helpers';

import { createFormSuggestionActions }     from '../form-suggestions/actions';

export type PropsType = {
  formType: string,
  fieldName: string,

  name: string,
  node: ?Object,
  showTables: boolean,
  andIdx: number,
  orIdx: number,
  suggestions: ?Object,
  datasetId: number,
  onCloseModal: Function,
  onUpdateLabel: Function,
  onToggleTable: Function,
  onSetFilterValue: Function,
  onSwitchFilterMode: Function,
  onResetAllFilters: Function
};

const mapStateToProps = (state, ownProps) => {
  const reduxFormState = selectReduxFormState(state);
  const conceptPosition = selectEditedConceptPosition(
    reduxFormState,
    ownProps.formType,
    ownProps.fieldName
  );

  const node = conceptPosition
    ? selectEditedConcept(reduxFormState, ownProps.formType, ownProps.fieldName, conceptPosition)
    : null;

  const { andIdx, orIdx } = conceptPosition || {};

  const showTables = node && node.tables && (
    node.tables.length > 1 ||
    node.tables.some(table => table.filters && table.filters.length > 0)
  );

  const formState = selectFormState(state, ownProps.formType);
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
    suggestions,

    onToggleTimestamps: () => {},
    onCloseModal: () => ownProps.onCloseModal(andIdx, orIdx),
    onUpdateLabel: (label) => ownProps.onUpdateLabel(andIdx, orIdx, label),
    onDropConcept: (concept) => ownProps.onDropConcept(andIdx, orIdx, concept),
    onRemoveConcept: (conceptId) => ownProps.onRemoveConcept(andIdx, orIdx, conceptId),
    onToggleTable: (...props) => ownProps.onToggleTable(andIdx, orIdx, ...props),
    onSetFilterValue: (...props) => ownProps.onSetFilterValue(andIdx, orIdx, ...props),
    onSwitchFilterMode: (...props) => ownProps.onSwitchFilterMode(andIdx, orIdx, ...props),
    onResetAllFilters: () => ownProps.onResetAllFilters(andIdx, orIdx),
  };
}

const mapDispatchToProps = (dispatch, ownProps) => {
  const {
    loadFormFilterSuggestions
  } = createFormSuggestionActions(ownProps.formType, ownProps.fieldName);

  return {
    onLoadFilterSuggestions: (...params) =>
      dispatch(loadFormFilterSuggestions(ownProps.formType, ownProps.fieldName, ...params))
  };
};

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...ownProps,
  ...stateProps,
  ...dispatchProps,
  onLoadFilterSuggestions: (datasetId, ...rest) =>
    dispatchProps.onLoadFilterSuggestions(
      datasetId,
      stateProps.andIdx,
      stateProps.orIdx,
      ...rest
    ),
});

const QueryNodeEditor = createConnectedQueryNodeEditor(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
);

export const FormQueryNodeEditor = (props: PropsType) =>
  <QueryNodeEditor
    type={`${props.formType}_${toUpperCaseUnderscore(props.fieldName)}`}
    {...props}
  />
