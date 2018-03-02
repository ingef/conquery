// @flow

import { createConnectedQueryNodeEditor }  from '../../query-node-editor';

import {
  selectFormState,
  selectReduxFormState,
}                                          from '../../external-forms/stateSelectors';

import { toUpperCaseUnderscore }           from '../../common/helpers';

import { createFormSuggestionActions }     from '../form-suggestions/actions';

import {
  selectEditedConceptPosition,
  selectEditedConcept,
  selectSuggestions,
} from '../selectors';

export type PropsType = {
  name: string,
  node: ?Object,
  showTables: boolean,
  andIdx: number,
  orIdx: number,
  suggestions: ?Object,
  datasetId: number,
  onCloseModal: Function,
  onToggleTable: Function,
  onSetFilterValue: Function,
  onSwitchFilterMode: Function,
  onResetAllFilters: Function
};

export const createConnectedFormQueryNodeEditor = (formType: string, fieldName: string) => {
  const mapStateToProps = (state, ownProps) => {
    const reduxFormState = selectReduxFormState(state);
    const conceptPosition = selectEditedConceptPosition(
      reduxFormState,
      formType,
      fieldName
    );

    const node = conceptPosition
      ? selectEditedConcept(reduxFormState, formType, fieldName, conceptPosition)
      : null;

    const { andIdx, orIdx } = conceptPosition || {};

    const showTables = node && node.tables && (
      node.tables.length > 1 ||
      node.tables.some(table => table.filters && table.filters.length > 0)
    );

    const formState = selectFormState(state, formType);
    const suggestions = conceptPosition
      ? selectSuggestions(formState, fieldName, conceptPosition)
      : null;

    return {
      node,
      andIdx,
      orIdx,
      editorState: formState[fieldName],
      isExcludeTimestampsPossible: false,
      showTables,
      suggestions,

      onToggleTimestamps: () => {},
      onCloseModal: () => ownProps.onCloseModal(andIdx, orIdx),
      onToggleTable: (...props) => ownProps.onToggleTable(andIdx, orIdx, ...props),
      onSetFilterValue: (...props) => ownProps.onSetFilterValue(andIdx, orIdx, ...props),
      onSwitchFilterMode: (...props) => ownProps.onSwitchFilterMode(andIdx, orIdx, ...props),
      onResetAllFilters: () => ownProps.onResetAllFilters(andIdx, orIdx),
    };
  }

  const mapDispatchToProps = (dispatch) => {
    const {
      loadFormFilterSuggestions
    } = createFormSuggestionActions(formType, fieldName);

    return {
      onLoadFilterSuggestions: (...params) =>
        dispatch(loadFormFilterSuggestions(formType, fieldName, ...params))
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

  return createConnectedQueryNodeEditor(
    `${formType}_${toUpperCaseUnderscore(fieldName)}`,
    mapStateToProps,
    mapDispatchToProps,
    mergeProps
  );
}
