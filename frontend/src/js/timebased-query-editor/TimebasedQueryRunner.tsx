import { connect } from "react-redux";

import actions from "../app/actions";
import QueryRunner from "../query-runner/QueryRunner";

import { allConditionsFilled } from "./helpers";

const { startTimebasedQuery, stopTimebasedQuery } = actions;

function isButtonEnabled(state, ownProps) {
  return !!(
    ownProps.datasetId !== null &&
    !state.timebasedQueryEditor.timebasedQueryRunner.startQuery.loading &&
    !state.timebasedQueryEditor.timebasedQueryRunner.stopQuery.loading &&
    allConditionsFilled(state.timebasedQueryEditor.timebasedQuery)
  );
}

const mapStateToProps = (state, ownProps) => ({
  queryRunner: state.timebasedQueryEditor.timebasedQueryRunner,
  isButtonEnabled: isButtonEnabled(state, ownProps),
  isQueryRunning: !!state.timebasedQueryEditor.timebasedQueryRunner
    .runningQuery,
  // Following ones only needed in dispatch functions
  queryId: state.timebasedQueryEditor.timebasedQueryRunner.runningQuery,
  version: state.conceptTrees.version,
  query: state.timebasedQueryEditor.timebasedQuery
});

const mapDispatchToProps = dispatch => ({
  startQuery: (datasetId, query, version) =>
    dispatch(startTimebasedQuery(datasetId, query, version)),
  stopQuery: (datasetId, queryId) =>
    dispatch(stopTimebasedQuery(datasetId, queryId))
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  startQuery: () =>
    dispatchProps.startQuery(
      ownProps.datasetId,
      stateProps.query,
      stateProps.version
    ),
  stopQuery: () =>
    dispatchProps.stopQuery(ownProps.datasetId, stateProps.queryId)
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
)(QueryRunner);
