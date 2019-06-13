// @flow

import { connect } from "react-redux";
import type { Dispatch } from "redux-thunk";
import { isValid, isPristine, getFormValues } from "redux-form";

import * as actions from "./actions";
import { selectReduxFormState } from "./stateSelectors";

import { QueryRunner } from "conquery/lib/js/query-runner";

const { startExternalFormsQuery, stopExternalFormsQuery } = actions;

const isActiveFormValid = state => {
  const form = getActiveForm(state);

  if (!form) return false;

  return (
    !isPristine(state.externalForms.activeForm, selectReduxFormState)(state) &&
    isValid(state.externalForms.activeForm, selectReduxFormState)(state)
  );
};

const isButtonEnabled = (state, ownProps) => {
  return !!(
    ownProps.datasetId !== null &&
    !state.externalForms.queryRunner.startQuery.loading &&
    !state.externalForms.queryRunner.stopQuery.loading &&
    isActiveFormValid(state)
  );
};

const getActiveForm = state => {
  return state.externalForms.reduxForm[state.externalForms.activeForm];
};

const mapStateToProps = (state, ownProps) => ({
  queryRunner: state.externalForms.queryRunner,
  isButtonEnabled: isButtonEnabled(state, ownProps),
  isQueryRunning: !!state.externalForms.queryRunner.runningQuery,
  // Following ones only needed in dispatch functions
  queryId: state.externalForms.queryRunner.runningQuery,
  version: state.categoryTrees.version,
  query: {
    formName: state.externalForms.activeForm,
    form: getFormValues(state.externalForms.activeForm, selectReduxFormState)(
      state
    )
  },
  formQueryTransformation: state.externalForms.activeForm
    ? state.externalForms.availableForms[state.externalForms.activeForm]
        .transformQueryToApi
    : query => query
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
