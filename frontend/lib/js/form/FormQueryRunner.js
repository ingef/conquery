// @flow

import { connect }       from 'react-redux';
import type { Dispatch } from 'redux-thunk';
import {
  isValid,
  isPristine,
  getFormValues,
}                        from 'redux-form';

import { QueryRunner }   from '../query-runner';
import * as actions      from './actions';

const {
  startFormQuery,
  stopFormQuery,
} = actions;

function isActiveFormValid(state) {
  const form = getActiveForm(state);

  if (!form) return false;

  return !isPristine(state.form.activeForm)(state.form) &&
         isValid(state.form.activeForm)(state.form);
}

function isButtonEnabled(state, ownProps) {
  return !!( // Return true or false even if all are undefined / null
    ownProps.datasetId !== null &&
    !state.form.queryRunner.startQuery.loading &&
    !state.form.queryRunner.stopQuery.loading &&
    isActiveFormValid(state)
  );
}

function getActiveForm(state) {
  return state.form.reduxForm[state.form.activeForm]
}


const mapStateToProps = (state, ownProps) => ({
  queryRunner: state.form.queryRunner,
  isButtonEnabled: isButtonEnabled(state, ownProps),
  isQueryRunning: !!state.form.queryRunner.runningQuery,
  // Following ones only needed in dispatch functions
  queryId: state.form.queryRunner.runningQuery,
  version: state.categoryTrees.version,
  query: {
    formName: state.form.activeForm,
    form: getFormValues(state.form.activeForm)(state.form),
  },
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  startQuery: (datasetId, query, version) =>
    dispatch(startFormQuery(datasetId, query, version)),
  stopQuery: (datasetId, queryId) =>
    dispatch(stopFormQuery(datasetId, queryId)),
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  startQuery: () => dispatchProps.startQuery(
    ownProps.datasetId,
    stateProps.query,
    stateProps.version,
  ),
  stopQuery: () => dispatchProps.stopQuery(
    ownProps.datasetId,
    stateProps.queryId,
  ),
});

export default connect(mapStateToProps, mapDispatchToProps, mergeProps)(QueryRunner);
