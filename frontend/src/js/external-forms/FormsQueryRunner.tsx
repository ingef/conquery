import React, { FC } from "react";
import { StateT } from "app-types";
import { useDispatch, useSelector } from "react-redux";
import { isValid, isPristine, getFormValues, FormStateMap } from "redux-form";

import QueryRunner from "../query-runner/QueryRunner";
import { DatasetIdT } from "../api/types";
import { QueryRunnerStateT } from "../query-runner/reducer";

import transformQueryToApi from "./transformQueryToApi";
import * as actions from "./actions";
import {
  selectReduxFormState,
  selectFormConfig,
  selectQueryRunner,
  selectRunningQuery,
  selectActiveFormType,
} from "./stateSelectors";
import { Form } from "./config-types";

const { startExternalFormsQuery, stopExternalFormsQuery } = actions;

const isActiveFormValid = (state: StateT) => {
  const activeForm = selectActiveFormType(state);
  const reduxFormState = selectReduxFormState(state);

  if (!activeForm || !reduxFormState) return false;

  return (
    !isPristine(activeForm, () => reduxFormState)(state) &&
    isValid(activeForm, () => reduxFormState)(state)
  );
};

const selectIsButtonEnabled = (
  datasetId: DatasetIdT,
  queryRunner: QueryRunnerStateT | null
) => (state: StateT) => {
  if (!queryRunner) return false;

  return !!(
    datasetId !== null &&
    !queryRunner.startQuery.loading &&
    !queryRunner.stopQuery.loading &&
    isActiveFormValid(state)
  );
};

interface PropsT {
  datasetId: DatasetIdT;
}

const FormQueryRunner: FC<PropsT> = ({ datasetId }) => {
  const queryRunner = useSelector<StateT, QueryRunnerStateT | null>(
    selectQueryRunner
  );
  const queryId = useSelector<StateT, string | number | null>(
    selectRunningQuery
  );
  const isQueryRunning = !!queryId;

  const isButtonEnabled = useSelector<StateT, boolean>(
    selectIsButtonEnabled(datasetId, queryRunner)
  );

  const formName = useSelector<StateT, string | null>(selectActiveFormType);
  const reduxFormState = useSelector<StateT, FormStateMap | null>(
    selectReduxFormState
  );
  const form = useSelector<StateT, unknown>((state) =>
    formName && reduxFormState
      ? getFormValues(formName, () => reduxFormState)(state)
      : {}
  );

  const formConfig = useSelector<StateT, Form | null>(selectFormConfig);

  const query = { formName, form };
  const formQueryTransformation = formConfig
    ? transformQueryToApi(formConfig)
    : () => {};

  const dispatch = useDispatch();
  const startQuery = () =>
    dispatch(
      startExternalFormsQuery(datasetId, query, {
        formQueryTransformation,
      })
    );
  const stopQuery = () => dispatch(stopExternalFormsQuery(datasetId, queryId));

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
