// @flow

import React              from 'react';
import type { Dispatch }  from 'redux-thunk';
import { connect }        from 'react-redux';

import { QueryNodeModal } from '../../query-node-modal';

import {
  capitalize
}                         from '../../common/helpers';

type PropsType = {
  node: ?Object,
  showTables: boolean,
  andIdx: number,
  orIdx: number,
  onCloseModal: Function,
  formType: string,
  clearFeatureNodeAction: Object,
  clearOutcomesNodeAction: Object,
  onResetAllFilters: Function,
  onToggleTimestamps: Function,
  // From here on injected by redux:
  onToggleTable: Function,
  onSetFilterValue: Function,
  onSwitchFilterMode: Function,
};

const FormQueryNodeModal = (props: PropsType) => (
  <QueryNodeModal
    node={props.node}
    showTables={props.showTables}
    andIdx={props.andIdx}
    orIdx={props.orIdx}
    onCloseModal={props.onCloseModal}

    onToggleTable={props.onToggleTable}
    onSetFilterValue={props.onSetFilterValue}
    onSwitchFilterMode={props.onSwitchFilterMode}
    onResetAllFilters={props.onResetAllFilters}
    onToggleTimestamps={props.onToggleTimestamps}
  />
);

function findNode(form, formName, name, andIdx, orIdx) {
  if (
    !form ||
    !form[formName] ||
    !form[formName].values ||
    !form[formName].values[name] ||
    !form[formName].values[name][andIdx]
  ) return null;

  const concept = form[formName].values[name][andIdx].concepts[orIdx];
  return findActiveFilters(concept)
    ? {...concept, hasActiveFilters: true }
    : concept;
}

const findActiveFilters = (concept) => (
  concept.tables.some(table =>
    table.exclude || (table.filters && table.filters.some(filter =>
      // multi select filters create an array that is not nulled when empty
      filter.value && Object.keys(filter.value).length > 0
    ))
  )
);

function mapStateToProps(state, ownProps) {
  const form = state.form[ownProps.formType][ownProps.name];
  const { andIdx, orIdx } = form;

  const node = findNode(state.form.reduxForm, ownProps.formType, ownProps.name, andIdx, orIdx);

  const showTables = node && node.tables && (
    node.tables.length > 1 ||
    node.tables.some(table => table.filters && table.filters.length > 0)
  );

  return {
    node,
    isExcludeTimestampsPossible: false,
    showTables,
    andIdx,
    orIdx,
  };
}

function mapDispatchToProps(dispatch: Dispatch, ownProps: Object) {
  return {
    onCloseModal: () => {
      if (ownProps.name === 'features')
        dispatch(ownProps.clearFeatureNodeAction());
      else if (ownProps.name === 'outcomes')
        dispatch(ownProps.clearOutcomesNodeAction());
    }
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(FormQueryNodeModal);
