import { useSelector } from "react-redux";

import type { DatasetT } from "../api/types";
import type { StateT } from "../app/reducers";
import { validateQueryLength } from "../model/query";
import QueryRunner from "../query-runner/QueryRunner";
import { useStartQuery, useStopQuery } from "../query-runner/actions";
import type { QueryRunnerStateT } from "../query-runner/reducer";

import type { StandardQueryStateT } from "./queryReducer";

function validateQueryStartStop({ startQuery, stopQuery }: QueryRunnerStateT) {
  return !startQuery.loading && !stopQuery.loading;
}

function validateDataset(datasetId: DatasetT["id"] | null) {
  return datasetId !== null;
}

const StandardQueryRunner = () => {
  const datasetId = useSelector<StateT, DatasetT["id"] | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const query = useSelector<StateT, StandardQueryStateT>(
    (state) => state.queryEditor.query,
  );
  const queryRunner = useSelector<StateT, QueryRunnerStateT>(
    (state) => state.queryEditor.queryRunner,
  );
  const selectedSecondaryId = useSelector<StateT, string | null>(
    (state) => state.queryEditor.selectedSecondaryId,
  );

  const queryId = queryRunner.runningQuery;

  const isDatasetValid = validateDataset(datasetId);
  const isQueryValid = validateQueryLength(query);
  const queryStartStopReady = validateQueryStartStop(queryRunner);

  const startStandardQuery = useStartQuery("standard");
  const stopStandardQuery = useStopQuery("standard");

  const startQuery = () => {
    if (datasetId) {
      startStandardQuery(datasetId, query, {
        selectedSecondaryId,
      });
    }
  };
  const stopQuery = () => {
    if (queryId) {
      stopStandardQuery(queryId);
    }
  };

  return (
    <QueryRunner
      queryRunner={queryRunner}
      disabled={!isDatasetValid || !isQueryValid || !queryStartStopReady}
      isQueryRunning={!!queryRunner.runningQuery}
      startQuery={startQuery}
      stopQuery={stopQuery}
    />
  );
};

export default StandardQueryRunner;
