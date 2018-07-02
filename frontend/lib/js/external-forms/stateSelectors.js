// @flow

import { nodeHasActiveFilters } from '../model/node';

const selectFormField = (state, formName, fieldName) => {
  if (
    !state ||
    !state[formName] ||
    !state[formName].values ||
    !state[formName].values[fieldName]
  ) return null;

  return state[formName].values[fieldName];
}

export const selectEditedConceptPosition = (state, formName, fieldName) => {
  const formField = selectFormField(state, formName, fieldName) || [];

  const selectedConcepts = formField.reduce((acc, group, andIdx) =>
      [...acc, ...group.concepts.map((concept, orIdx) => ({andIdx, orIdx, concept}))], [])
      .filter(({concept}) => concept && concept.isEditing)
      .map(({andIdx, orIdx}) => ({andIdx, orIdx}));

  return selectedConcepts.length ? selectedConcepts[0] : null;
}

export const selectEditedConcept = (state, formName, fieldName, {andIdx, orIdx}) => {
  const formField = selectFormField(state, formName, fieldName);
  const concept = formField[andIdx].concepts[orIdx];
  return nodeHasActiveFilters(concept)
    ? {...concept, hasActiveFilters: true }
    : concept;
}

export const selectSuggestions = (state, fieldName, {andIdx, orIdx}) => {
  return state.suggestions &&
    state.suggestions[fieldName] &&
    state.suggestions[fieldName][andIdx] &&
    state.suggestions[fieldName][andIdx][orIdx];
};

export const selectFormState = (state: Object, formType: string) => state.externalForms[formType];
export const selectReduxFormState = (state: Object) => state.externalForms.reduxForm;
