import { connect } from "react-redux";
import type { Dispatch } from "redux-thunk";
import { isValid, isPristine, getFormValues } from "redux-form";

import transformQueryToApi from "./transformQueryToApi";
import * as actions from "./actions";
import {
  selectReduxFormState,
  selectFormConfig,
  selectQueryRunner,
  selectRunningQuery,
  selectActiveForm
} from "./stateSelectors";

import QueryRunner from "../query-runner/QueryRunner";

const { startExternalFormsQuery, stopExternalFormsQuery } = actions;

const isActiveFormValid = state => {
  const activeForm = selectActiveForm(state);

  if (!activeForm) return false;

  return (
    !isPristine(activeForm, selectReduxFormState)(state) &&
    isValid(activeForm, selectReduxFormState)(state)
  );
};

const isButtonEnabled = (state, ownProps) => {
  const queryRunner = selectQueryRunner(state);

  if (!queryRunner) return false;

  return !!(
    ownProps.datasetId !== null &&
    !queryRunner.startQuery.loading &&
    !queryRunner.stopQuery.loading &&
    isActiveFormValid(state)
  );
};

const mapStateToProps = (state, ownProps) => ({
  queryRunner: selectQueryRunner(state),
  isButtonEnabled: isButtonEnabled(state, ownProps),
  isQueryRunning: !!selectRunningQuery(state),
  // Following ones only needed in dispatch functions
  queryId: selectRunningQuery(state),
  version: state.conceptTrees.version,
  query: {
    formName: selectActiveForm(state),
    form: selectActiveForm(state)
      ? getFormValues(selectActiveForm(state), selectReduxFormState)(state)
      : {}
  },
  formQueryTransformation: transformQueryToApi(selectFormConfig(state))
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  startQuery: (datasetId, query, version, formQueryTransformation) =>
    dispatch(
      startExternalFormsQuery(
        datasetId,
        query,
        version,
        formQueryTransformation
      )
    ),
  stopQuery: (datasetId, queryId) =>
    dispatch(stopExternalFormsQuery(datasetId, queryId))
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  startQuery: () =>
    dispatchProps.startQuery(
      ownProps.datasetId,
      stateProps.query,
      stateProps.version,
      stateProps.formQueryTransformation
    ),
  stopQuery: () =>
    dispatchProps.stopQuery(ownProps.datasetId, stateProps.queryId)
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
)(QueryRunner);
