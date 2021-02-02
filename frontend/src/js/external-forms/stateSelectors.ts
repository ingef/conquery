import { initTables } from "./transformers";
import { useSelector } from "react-redux";
import { StateT } from "app-types";
import { FormContextStateT } from "./reducer";
import { getLocale } from "../localization";
import { Form } from "./config-types";

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
        ...group.concepts.map((concept, orIdx) => ({ andIdx, orIdx, concept })),
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

export const selectSuggestions = (
  state: FormContextStateT,
  fieldName: string,
  { andIdx, orIdx }: { andIdx: number; orIdx: number }
) => {
  return (
    state.suggestions &&
    state.suggestions[fieldName] &&
    state.suggestions[fieldName][andIdx] &&
    state.suggestions[fieldName][andIdx][orIdx]
  );
};

export const selectFormContextState = (state: StateT, formType: string) =>
  state.externalForms ? state.externalForms.formsContext[formType] : null;

export const selectReduxForm = (state: StateT) =>
  state.externalForms ? state.externalForms.reduxForm : null;

export const selectActiveFormValues = (state: StateT): Record<string, any> => {
  const reduxForm = selectReduxForm(state);
  const activeForm = selectActiveFormType(state);

  return reduxForm && activeForm && !!reduxForm[activeForm]
    ? reduxForm[activeForm].values
    : {};
};

export const selectAvailableForms = (state: StateT) =>
  state.externalForms ? state.externalForms.availableForms : {};

export const selectActiveFormType = (state: StateT) =>
  state.externalForms ? state.externalForms.activeForm : null;

export const useActiveFormType = () =>
  useSelector<StateT, string | null>((state) => selectActiveFormType(state));

export const selectFormConfig = (state: StateT): Form | null => {
  const availableForms = selectAvailableForms(state);
  const activeFormType = selectActiveFormType(state);

  return (activeFormType && availableForms[activeFormType]) || null;
};

export const selectActiveFormName = (state: StateT): string => {
  const formConfig = selectFormConfig(state);
  const locale = getLocale();

  return (formConfig && formConfig.title[locale]) || "";
};

export const selectReduxFormState = (state: StateT) =>
  state.externalForms ? state.externalForms.reduxForm : null;

export const selectQueryRunner = (state: StateT) =>
  state.externalForms ? state.externalForms.queryRunner : null;

export const selectRunningQuery = (state: StateT) => {
  const queryRunner = selectQueryRunner(state);

  return queryRunner ? queryRunner.runningQuery : null;
};

function getVisibleConceptListFields(config, values) {
  const topLevelFields = config.fields.filter(
    (field) => field.type === "CONCEPT_LIST"
  );
  const tabFields = config.fields.filter((field) => field.type === "TABS");

  const fieldsWithinVisibleTabs = tabFields.reduce((fields, tabField) => {
    const activeTabName = values[tabField.name];
    const activeTab = tabField.tabs.find((tab) => tab.name === activeTabName);

    const activeTabConceltListFields = activeTab
      ? getVisibleConceptListFields(activeTab)
      : [];

    return [...fields, ...activeTabConceltListFields];
  }, []);

  return [...topLevelFields, ...fieldsWithinVisibleTabs];
}

export const useVisibleConceptListFields = () => {
  const config = useSelector((state) => selectFormConfig(state));
  const values = useSelector((state) => selectActiveFormValues(state));

  if (!config) return false;

  return getVisibleConceptListFields(config, values);
};

export const useAllowExtendedCopying = (targetFieldname: string) => {
  const values = useSelector((state) => selectActiveFormValues(state));
  const otherConceptListFields = useVisibleConceptListFields().filter(
    (field) => field.name !== targetFieldname
  );

  // Need to have min 2 fields to copy from one to another
  if (otherConceptListFields.length < 1) return false;

  const fieldHasFilledConcept = (field) =>
    !!values[field.name] &&
    values[field.name].some((value) =>
      value.concepts.some((concept) => !!concept)
    );

  return otherConceptListFields.some(fieldHasFilledConcept);
};

export const useFormLabelByType = (formType: string) => {
  const availableForms = useSelector<StateT, { [formName: string]: Form }>(
    (state) => selectAvailableForms(state)
  );
  const locale = getLocale();

  return availableForms[formType]
    ? availableForms[formType].title[locale]
    : formType;
};
