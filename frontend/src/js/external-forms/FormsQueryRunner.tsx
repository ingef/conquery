import { StateT } from "app-types";
import { FC } from "react";
import { useFormContext } from "react-hook-form";
import { useSelector } from "react-redux";
import { isValid, isPristine, getFormValues, FormStateMap } from "redux-form";

import { DatasetIdT } from "../api/types";
import { useDatasetId } from "../dataset/selectors";
import QueryRunner from "../query-runner/QueryRunner";
import { useStartQuery, useStopQuery } from "../query-runner/actions";
import { QueryRunnerStateT } from "../query-runner/reducer";

import { Form } from "./config-types";
import {
  selectReduxFormState,
  selectFormConfig,
  selectQueryRunner,
  selectRunningQuery,
  selectActiveFormType,
} from "./stateSelectors";
import transformQueryToApi from "./transformQueryToApi";

const isActiveFormValid = (state: StateT) => {
  const activeForm = selectActiveFormType(state);
  const reduxFormState = selectReduxFormState(state);

  if (!activeForm || !reduxFormState) return false;

  return (
    !isPristine(activeForm, () => reduxFormState)(state) &&
    isValid(activeForm, () => reduxFormState)(state)
  );
};

const selectIsButtonEnabled =
  (datasetId: DatasetIdT | null, queryRunner: QueryRunnerStateT | null) =>
  (state: StateT) => {
    if (!queryRunner) return false;

    return !!(
      datasetId !== null &&
      !queryRunner.startQuery.loading &&
      !queryRunner.stopQuery.loading &&
      isActiveFormValid(state)
    );
  };

const FormQueryRunner: FC = () => {
  const datasetId = useDatasetId();
  const queryRunner = useSelector<StateT, QueryRunnerStateT | null>(
    selectQueryRunner,
  );
  const queryId = useSelector<StateT, string | null>(selectRunningQuery);
  const isQueryRunning = !!queryId;

  const isButtonEnabled = useSelector<StateT, boolean>(
    selectIsButtonEnabled(datasetId, queryRunner),
  );

  const formName = useSelector<StateT, string | null>(selectActiveFormType);
  const formContext = useFormContext();
  const form = formName ? formContext.getValues() : {};

  const formConfig = useSelector<StateT, Form | null>(selectFormConfig);

  const query = { formName, form };
  const formQueryTransformation = formConfig
    ? transformQueryToApi(formConfig)
    : () => {};

  const startExternalFormsQuery = useStartQuery("externalForms");
  const stopExternalFormsQuery = useStopQuery("externalForms");

  const startQuery = () => {
    if (datasetId) {
      startExternalFormsQuery(datasetId, query, {
        formQueryTransformation,
      });
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
      isButtonEnabled={isButtonEnabled}
      isQueryRunning={isQueryRunning}
      startQuery={startQuery}
      stopQuery={stopQuery}
    />
  );
};

export default FormQueryRunner;
