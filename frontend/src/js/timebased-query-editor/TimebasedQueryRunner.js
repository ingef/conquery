import { connect }             from 'react-redux';

import actions                 from '../app/actions';
import { QueryRunner }         from '../query-runner';

import { allConditionsFilled } from './helpers';

const {
  startTimebasedQuery,
  stopTimebasedQuery,
} = actions;

function isButtonEnabled(state, ownProps) {
  return !!(
    ownProps.datasetId !== null &&
    !state.timebasedQueryRunner.startQuery.loading &&
    !state.timebasedQueryRunner.stopQuery.loading &&
    allConditionsFilled(state.timebasedQuery)
  );
}

const mapStateToProps = (state, ownProps) => ({
  queryRunner: state.timebasedQueryRunner,
  isButtonEnabled: isButtonEnabled(state, ownProps),
  isQueryRunning: !!state.timebasedQueryRunner.runningQuery,
  // Following ones only needed in dispatch functions
  queryId: state.timebasedQueryRunner.runningQuery,
  version: state.categoryTrees.version,
  query: state.timebasedQuery,
});

const mapDispatchToProps = (dispatch) => ({
  startQuery: (datasetId, query, version) =>
    dispatch(startTimebasedQuery(datasetId, query, version)),
  stopQuery: (datasetId, queryId) =>
    dispatch(stopTimebasedQuery(datasetId, queryId)),
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
