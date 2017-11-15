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
  startStatisticsQuery,
  stopStatisticsQuery,
} = actions;

function isActiveFormValid(state) {
  const form = getActiveForm(state);

  if (!form) return false;

  return !isPristine(state.statistics.activeForm)(state.statistics) &&
         isValid(state.statistics.activeForm)(state.statistics);
}

function isButtonEnabled(state) {
  return !!( // Return true or false even if all are undefined / null
    !state.statistics.queryRunner.startQuery.loading &&
    !state.statistics.queryRunner.stopQuery.loading &&
    isActiveFormValid(state)
  );
}

function getActiveForm(state) {
  return state.statistics.form[state.statistics.activeForm]
}


const mapStateToProps = (state) => ({
  queryRunner: state.statistics.queryRunner,
  isButtonEnabled: isButtonEnabled(state),
  isQueryRunning: !!state.statistics.queryRunner.runningQuery,
  // Following ones only needed in dispatch functions
  queryId: state.statistics.queryRunner.runningQuery,
  version: state.categoryTrees.version,
  query: {
    formName: state.statistics.activeForm,
    form: getFormValues(state.statistics.activeForm)(state.statistics),
  },
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  startQuery: (datasetId, query, version) =>
    dispatch(startStatisticsQuery(datasetId, query, version)),
  stopQuery: (datasetId, queryId) =>
    dispatch(stopStatisticsQuery(datasetId, queryId)),
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  isButtonEnabled: stateProps.isButtonEnabled && ownProps.datasetId !== null,
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
