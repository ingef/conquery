import { StateT } from "app-types";
import { FC } from "react";
import { useSelector } from "react-redux";

import { DatasetIdT, QueryIdT } from "../api/types";
import QueryRunner from "../query-runner/QueryRunner";
import { useStartQuery, useStopQuery } from "../query-runner/actions";
import { QueryRunnerStateT } from "../query-runner/reducer";

import { allConditionsFilled } from "./helpers";
import { TimebasedQueryStateT } from "./reducer";

const selectIsButtonEnabled =
  (datasetId: DatasetIdT | null, queryRunner: QueryRunnerStateT | null) =>
  (state: StateT) => {
    if (!queryRunner) return false;

    return !!(
      datasetId !== null &&
      !queryRunner.startQuery.loading &&
      !queryRunner.stopQuery.loading &&
      allConditionsFilled(state.timebasedQueryEditor.timebasedQuery)
    );
  };

const TimebasedQueryRunner = () => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const queryRunner = useSelector<StateT, QueryRunnerStateT>(
    (state) => state.timebasedQueryEditor.timebasedQueryRunner,
  );
  const isButtonEnabled = useSelector<StateT, boolean>(
    selectIsButtonEnabled(datasetId, queryRunner),
  );
  const isQueryRunning = !!queryRunner.runningQuery;
  // Following ones only needed in dispatch functions
  const queryId = useSelector<StateT, QueryIdT | null>(
    (state) => state.timebasedQueryEditor.timebasedQueryRunner.runningQuery,
  );
  const query = useSelector<StateT, TimebasedQueryStateT>(
    (state) => state.timebasedQueryEditor.timebasedQuery,
  );

  const startTimebasedQuery = useStartQuery("timebased");
  const stopTimebasedQuery = useStopQuery("timebased");

  const startQuery = () => {
    if (datasetId) {
      startTimebasedQuery(datasetId, query);
    }
  };
  const stopQuery = () => {
    if (datasetId && queryId) {
      stopTimebasedQuery(datasetId, queryId);
    }
  };

  return (
    <QueryRunner
      queryRunner={queryRunner}
      isButtonEnabled={isButtonEnabled}
      isQueryRunning={isQueryRunning}
      startQuery={startQuery}
      stopQuery={stopQuery}
    />
  );
};

export default TimebasedQueryRunner;
