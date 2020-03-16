// @flow

import { initTables } from "./transformers";

const selectFormField = (state, formName, fieldName) => {
  if (
    !state ||
    !state[formName] ||
    !state[formName].values ||
    !state[formName].values[fieldName]
  )
    return null;

  return state[formName].values[fieldName];
};

export const selectEditedConceptPosition = (state, formName, fieldName) => {
  const formField = selectFormField(state, formName, fieldName) || [];

  const selectedConcepts = formField
    .reduce(
      (acc, group, andIdx) => [
        ...acc,
        ...group.concepts.map((concept, orIdx) => ({ andIdx, orIdx, concept }))
      ],
      []
    )
    .filter(({ concept }) => concept && concept.isEditing)
    .map(({ andIdx, orIdx }) => ({ andIdx, orIdx }));

  return selectedConcepts.length ? selectedConcepts[0] : null;
};

export const selectEditedConcept = (
  state,
  formName,
  fieldName,
  { andIdx, orIdx },
  blacklistedTables: string[],
  whitelistedTables: string[]
) => {
  const formField = selectFormField(state, formName, fieldName);
  const concept = formField[andIdx].concepts[orIdx];

  return initTables(blacklistedTables, whitelistedTables)(concept);
};

export const selectSuggestions = (state, fieldName, { andIdx, orIdx }) => {
  return (
    state.suggestions &&
    state.suggestions[fieldName] &&
    state.suggestions[fieldName][andIdx] &&
    state.suggestions[fieldName][andIdx][orIdx]
  );
};

export const selectFormState = (state: Object, formType: string) =>
  state.externalForms ? state.externalForms[formType] : null;

export const selectReduxForm = (state: Object) =>
  state.externalForms ? state.externalForms.reduxForm : null;

export const selectActiveFormValues = (state: Object) => {
  const reduxForm = selectReduxForm(state);
  const activeForm = selectActiveForm(state);

  return reduxForm ? reduxForm[activeForm] : {};
};

export const selectAvailableForms = (state: Object) =>
  state.externalForms ? state.externalForms.availableForms : [];

export const selectActiveForm = (state: Object) =>
  state.externalForms ? state.externalForms.activeForm : null;

export const selectFormConfig = (state: Object) => {
  const availableForms = selectAvailableForms(state);
  const activeForm = selectActiveForm(state);

  return activeForm ? availableForms[activeForm] : null;
};

export const selectReduxFormState = (state: Object) =>
  state.externalForms ? state.externalForms.reduxForm : null;

export const selectQueryRunner = (state: Object) =>
  state.externalForms ? state.externalForms.queryRunner : null;

export const selectRunningQuery = (state: Object) => {
  const queryRunner = selectQueryRunner(state);

  return queryRunner ? queryRunner.runningQuery : null;
};
