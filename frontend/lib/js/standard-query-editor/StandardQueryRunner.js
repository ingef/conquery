// @flow

import { connect }     from 'react-redux';
import type { Dispatch } from 'redux-thunk';

import { QueryRunner } from '../query-runner';
import actions         from '../app/actions';

const {
  startStandardQuery,
  stopStandardQuery,
} = actions;

function isButtonEnabled(state) {
  return !!( // Return true or false even if all are undefined / null
    state.datasets.selectedDatasetId !== undefined &&
    !state.standardQueryRunner.startQuery.loading &&
    !state.standardQueryRunner.stopQuery.loading &&
    state.query.length > 0
  );
}


const mapStateToProps = (state) => ({
  queryRunner: state.standardQueryRunner,
  isButtonEnabled: isButtonEnabled(state),
  isQueryRunning: !!state.standardQueryRunner.runningQuery,
  // Following ones only needed in dispatch functions
  queryId: state.standardQueryRunner.runningQuery,
  version: state.categoryTrees.version,
  query: state.query,
  datasetId: state.datasets.selectedDatasetId,
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  startQuery: (datasetId, query, version) =>
    dispatch(startStandardQuery(datasetId, query, version)),
  stopQuery: (datasetId, queryId) =>
    dispatch(stopStandardQuery(datasetId, queryId)),
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  startQuery: () => dispatchProps.startQuery(
    stateProps.datasetId,
    stateProps.query,
    stateProps.version,
  ),
  stopQuery: () => dispatchProps.stopQuery(
    stateProps.datasetId,
    stateProps.queryId,
  ),
});

export default connect(mapStateToProps, mapDispatchToProps, mergeProps)(QueryRunner);
