import { StateT } from "app-types";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { DatasetIdT } from "../api/types";
import { validateQueryLength, validateQueryDates } from "../model/query";
import QueryRunner from "../query-runner/QueryRunner";
import { useStartQuery, useStopQuery } from "../query-runner/actions";
import type { QueryRunnerStateT } from "../query-runner/reducer";

import type { StandardQueryStateT } from "./queryReducer";

function validateQueryStartStop({ startQuery, stopQuery }: QueryRunnerStateT) {
  return !startQuery.loading && !stopQuery.loading;
}

function validateDataset(datasetId: DatasetIdT | null) {
  return datasetId !== null;
}

function useButtonTooltip(hasQueryValidDates: boolean) {
  const { t } = useTranslation();

  if (!hasQueryValidDates) {
    return t("queryRunner.errorDates");
  }

  // Potentially add further validation and more detailed messages

  return undefined;
}

const StandardQueryRunner = () => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
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
  const hasQueryValidDates = validateQueryDates(query);
  const isQueryValid = validateQueryLength(query) && hasQueryValidDates;
  const isQueryNotStartedOrStopped = validateQueryStartStop(queryRunner);

  const buttonTooltip = useButtonTooltip(hasQueryValidDates);

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
    if (datasetId && queryId) {
      stopStandardQuery(datasetId, queryId);
    }
  };

  return (
    <QueryRunner
      queryRunner={queryRunner}
      buttonTooltip={buttonTooltip}
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
