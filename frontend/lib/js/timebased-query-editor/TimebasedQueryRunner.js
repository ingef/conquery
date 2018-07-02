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
    !state.panes.right.tabs.timebasedQueryEditor.timebasedQueryRunner.startQuery.loading &&
    !state.panes.right.tabs.timebasedQueryEditor.timebasedQueryRunner.stopQuery.loading &&
    allConditionsFilled(state.panes.right.tabs.timebasedQueryEditor.timebasedQuery)
  );
}

const mapStateToProps = (state, ownProps) => ({
  queryRunner: state.panes.right.tabs.timebasedQueryEditor.timebasedQueryRunner,
  isButtonEnabled: isButtonEnabled(state, ownProps),
  isQueryRunning: !!state.panes.right.tabs.timebasedQueryEditor.timebasedQueryRunner.runningQuery,
  // Following ones only needed in dispatch functions
  queryId: state.panes.right.tabs.timebasedQueryEditor.timebasedQueryRunner.runningQuery,
  version: state.categoryTrees.version,
  query: state.panes.right.tabs.timebasedQueryEditor.timebasedQuery,
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
