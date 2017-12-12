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

import {
  selectReduxFormState
}                        from './stateSelectors';


const {
  startExternalFormsQuery,
  stopExternalFormsQuery,
} = actions;

function isActiveFormValid(state) {
  const form = getActiveForm(state);

  if (!form) return false;

  return !isPristine(state.externalForms.activeForm, selectReduxFormState)(state) &&
         isValid(state.externalForms.activeForm, selectReduxFormState)(state);
}

function isButtonEnabled(state, ownProps) {
  return !!( // Return true or false even if all are undefined / null
    ownProps.datasetId !== null &&
    !state.externalForms.queryRunner.startQuery.loading &&
    !state.externalForms.queryRunner.stopQuery.loading &&
    isActiveFormValid(state)
  );
}

function getActiveForm(state) {
  return state.externalForms.reduxForm[state.externalForms.activeForm]
}


const mapStateToProps = (state, ownProps) => ({
  queryRunner: state.externalForms.queryRunner,
  isButtonEnabled: isButtonEnabled(state, ownProps),
  isQueryRunning: !!state.externalForms.queryRunner.runningQuery,
  // Following ones only needed in dispatch functions
  queryId: state.externalForms.queryRunner.runningQuery,
  version: state.categoryTrees.version,
  query: {
    formName: state.externalForms.activeForm,
    form: getFormValues(state.externalForms.activeForm, selectReduxFormState)(state),
  },
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  startQuery: (datasetId, query, version) =>
    dispatch(startExternalFormsQuery(datasetId, query, version)),
  stopQuery: (datasetId, queryId) =>
    dispatch(stopExternalFormsQuery(datasetId, queryId)),
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
