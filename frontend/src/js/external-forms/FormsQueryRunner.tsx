import { StateT } from "app-types";
import { FC } from "react";
import { useFormContext, useFormState } from "react-hook-form";
import { useSelector } from "react-redux";

import { DatasetIdT } from "../api/types";
import { exists } from "../common/helpers/exists";
import { useDatasetId } from "../dataset/selectors";
import QueryRunner from "../query-runner/QueryRunner";
import { useStartQuery, useStopQuery } from "../query-runner/actions";
import { QueryRunnerStateT } from "../query-runner/reducer";

import { Form } from "./config-types";
import {
  selectFormConfig,
  selectQueryRunner,
  selectRunningQuery,
} from "./stateSelectors";
import transformQueryToApi from "./transformQueryToApi";

const isButtonEnabled = ({
  queryRunner,
  datasetId,
  isValid,
}: {
  datasetId: DatasetIdT | null;
  queryRunner: QueryRunnerStateT | null;
  isValid: boolean;
}) => {
  if (!queryRunner) return false;

  return !!(
    datasetId !== null &&
    !queryRunner.startQuery.loading &&
    !queryRunner.stopQuery.loading &&
    isValid
  );
};

const FormQueryRunner: FC = () => {
  const datasetId = useDatasetId();
  const queryRunner = useSelector<StateT, QueryRunnerStateT | null>(
    selectQueryRunner,
  );
  const queryId = useSelector<StateT, string | null>(selectRunningQuery);
  const isQueryRunning = exists(queryId);
  const formConfig = useSelector<StateT, Form | null>(selectFormConfig);

  const { getValues } = useFormContext();
  const { isValid } = useFormState();

  const buttonEnabled =
    exists(formConfig) &&
    isButtonEnabled({
      datasetId,
      queryRunner,
      isValid,
    });

  const startExternalFormsQuery = useStartQuery("externalForms");
  const stopExternalFormsQuery = useStopQuery("externalForms");

  const startQuery = () => {
    if (datasetId && exists(formConfig)) {
      const values = getValues();

      startExternalFormsQuery(
        datasetId,
        transformQueryToApi(formConfig, values),
      );
    }
  };
  const stopQuery = () => {
    if (datasetId && queryId) {
      stopExternalFormsQuery(datasetId, queryId);
    }
  };

  if (!queryRunner) {
    return null;
  }

  return (
    <QueryRunner
      queryRunner={queryRunner}
      isButtonEnabled={buttonEnabled}
      isQueryRunning={isQueryRunning}
      startQuery={startQuery}
      stopQuery={stopQuery}
    />
  );
};

export default FormQueryRunner;
