// @flow

import React from "react";

import {
  selectFormState,
  selectReduxFormState,
  selectEditedConceptPosition,
  selectEditedConcept,
  selectSuggestions
} from "../stateSelectors";
import { createFormSuggestionActions } from "../form-suggestions/actions";

import { createConnectedQueryNodeEditor } from "conquery/lib/js/query-node-editor";
import { toUpperCaseUnderscore } from "conquery/lib/js/common/helpers";
import { hasConceptChildren } from "conquery/lib/js/concept-trees/globalTreeStoreHelper";

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
  onResetAllFilters: Function,
  onDropFiles: Function,
  onToggleIncludeSubnodes: Function
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
        conceptPosition
      )
    : null;

  const { andIdx, orIdx } = conceptPosition || {};

  const showTables =
    node &&
    node.tables &&
    (node.tables.length > 1 ||
      node.tables.some(table => table.filters && table.filters.length > 0));

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
    canIncludeSubnodes: hasConceptChildren(node),
    showTables,
    suggestions,
    currencyConfig: state.startup.config.currency,

    onToggleTimestamps: () => {},
    onToggleIncludeSubnodes: includeSubnodes =>
      ownProps.onToggleIncludeSubnodes(andIdx, orIdx, includeSubnodes),
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
    onDropFiles: (
      datasetId,
      tree,
      tableIdx,
      tableId,
      filterIdx,
      filterId,
      files
    ) =>
      ownProps.onDropFiles(
        andIdx,
        orIdx,
        tableId,
        tableIdx,
        filterId,
        filterIdx,
        files
      )
  };
};

const mapDispatchToProps = (dispatch, ownProps) => {
  const { loadFormFilterSuggestions } = createFormSuggestionActions(
    ownProps.formType,
    ownProps.fieldName
  );

  return {
    onLoadFilterSuggestions: (...params) =>
      dispatch(
        loadFormFilterSuggestions(
          ownProps.formType,
          ownProps.fieldName,
          ...params
        )
      )
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
