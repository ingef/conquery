import { initTables } from "./transformers";
import { useSelector } from "react-redux";

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

  return reduxForm && activeForm ? reduxForm[activeForm].values : {};
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

function getVisibleConceptListFields(config, values) {
  const topLevelFields = config.fields.filter(
    field => field.type === "CONCEPT_LIST"
  );
  const tabFields = config.fields.filter(field => field.type === "TABS");

  const fieldsWithinVisibleTabs = tabFields.reduce((fields, tabField) => {
    const activeTabName = values[tabField.name];
    const activeTab = tabField.tabs.find(tab => tab.name === activeTabName);

    const activeTabConceltListFields = activeTab
      ? getVisibleConceptListFields(activeTab)
      : [];

    return [...fields, ...activeTabConceltListFields];
  }, []);

  return [...topLevelFields, ...fieldsWithinVisibleTabs];
}

export const useVisibleConceptListFields = () => {
  const config = useSelector(state => selectFormConfig(state));
  const values = useSelector(state => selectActiveFormValues(state));

  if (!config) return false;

  return getVisibleConceptListFields(config, values);
};

export const useAllowExtendedCopying = (targetFieldname: string) => {
  const values = useSelector(state => selectActiveFormValues(state));
  const otherConceptListFields = useVisibleConceptListFields().filter(
    field => field.name !== targetFieldname
  );

  // Need to have min 2 fields to copy from one to another
  if (otherConceptListFields.length < 1) return false;

  const fieldHasFilledConcept = field =>
    !!values[field.name] &&
    values[field.name].some(value => value.concepts.some(concept => !!concept));

  return otherConceptListFields.some(fieldHasFilledConcept);
};
