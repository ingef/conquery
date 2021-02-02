import React, { FC } from "react";
import { useDispatch, useSelector } from "react-redux";
import { StateT } from "app-types";

import QueryRunner from "../query-runner/QueryRunner";

import { validateQueryLength, validateQueryDates } from "../model/query";

import actions from "../app/actions";

import type { DatasetIdT } from "../api/types";
import type { QueryRunnerStateT } from "../query-runner/reducer";
import type { StandardQueryStateT } from "./queryReducer";

const { startStandardQuery, stopStandardQuery } = actions;

function validateQueryStartStop({ startQuery, stopQuery }: QueryRunnerStateT) {
  return !startQuery.loading && !stopQuery.loading;
}

function validateDataset(datasetId: DatasetIdT) {
  return datasetId !== null;
}

function getButtonTooltipKey(hasQueryValidDates: boolean) {
  if (!hasQueryValidDates) {
    return "queryRunner.errorDates";
  }

  // Potentially add further validation and more detailed messages

  return null;
}

interface PropsT {
  datasetId: DatasetIdT;
}

const StandardQueryRunner: FC<PropsT> = ({ datasetId }) => {
  const query = useSelector<StateT, StandardQueryStateT>(
    (state) => state.queryEditor.query
  );
  const queryRunner = useSelector<StateT, QueryRunnerStateT>(
    (state) => state.queryEditor.queryRunner
  );
  const selectedSecondaryId = useSelector<StateT, string | null>(
    (state) => state.queryEditor.selectedSecondaryId
  );

  const queryId = queryRunner.runningQuery;

  const isDatasetValid = validateDataset(datasetId);
  const hasQueryValidDates = validateQueryDates(query);
  const isQueryValid = validateQueryLength(query) && hasQueryValidDates;
  const isQueryNotStartedOrStopped = validateQueryStartStop(queryRunner);

  const dispatch = useDispatch();

  const startQuery = () =>
    dispatch(
      startStandardQuery(datasetId, query, {
        selectedSecondaryId,
      })
    );
  const stopQuery = () => dispatch(stopStandardQuery(datasetId, queryId));

  return (
    <QueryRunner
      queryRunner={queryRunner}
      buttonTooltipKey={getButtonTooltipKey(hasQueryValidDates)}
      isButtonEnabled={
        isDatasetValid && isQueryValid && isQueryNotStartedOrStopped
      }
      isQueryRunning={!!queryRunner.runningQuery}
      startQuery={startQuery}
      stopQuery={stopQuery}
    />
  );
};

export default StandardQueryRunner;
