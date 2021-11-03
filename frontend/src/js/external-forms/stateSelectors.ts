import { StateT } from "app-types";
import { useSelector } from "react-redux";

import { exists } from "../common/helpers/exists";
import { useActiveLang } from "../localization/useActiveLang";

import { ConceptListField, Form, GeneralField, Tabs } from "./config-types";
import { initTables } from "./transformers";

const selectFormField = (state, formName: string, fieldName: string) => {
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
      [],
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
  blocklistedTables: string[],
  allowlistedTables: string[],
) => {
  const formField = selectFormField(state, formName, fieldName);
  const concept = formField[andIdx].concepts[orIdx];

  return initTables({ blocklistedTables, allowlistedTables })(concept);
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

export const useSelectActiveFormName = (): string => {
  const formConfig = useSelector<StateT, Form | null>((state) =>
    selectFormConfig(state),
  );
  const activeLang = useActiveLang();

  return (formConfig && formConfig.title[activeLang]) || "";
};

export const selectReduxFormState = (state: StateT) =>
  state.externalForms ? state.externalForms.reduxForm : {};

export const selectQueryRunner = (state: StateT) =>
  state.externalForms ? state.externalForms.queryRunner : null;

export const selectRunningQuery = (state: StateT) => {
  const queryRunner = selectQueryRunner(state);

  return queryRunner ? queryRunner.runningQuery : null;
};

function getVisibleConceptListFields(
  config: { fields: GeneralField[] },
  values: Record<string, any>,
): ConceptListField[] {
  const topLevelFields = config.fields.filter(
    (field): field is ConceptListField => field.type === "CONCEPT_LIST",
  );
  const tabFields = config.fields.filter(
    (field): field is Tabs => field.type === "TABS",
  );

  const fieldsWithinVisibleTabs = tabFields.reduce<ConceptListField[]>(
    (fields, tabField) => {
      const activeTabName = values[tabField.name];
      const activeTab = tabField.tabs.find((tab) => tab.name === activeTabName);

      const activeTabConceptListFields = activeTab
        ? getVisibleConceptListFields(activeTab, values)
        : [];

      return [...fields, ...activeTabConceptListFields];
    },
    [],
  );

  return [...topLevelFields, ...fieldsWithinVisibleTabs];
}

export const useVisibleConceptListFields = () => {
  const config = useSelector<StateT, Form | null>((state) =>
    selectFormConfig(state),
  );
  const values = useSelector<StateT, Record<string, any>>((state) =>
    selectActiveFormValues(state),
  );

  if (!config) return [];

  return getVisibleConceptListFields(config, values);
};

export const useAllowExtendedCopying = (targetFieldname: string) => {
  const values = useSelector<StateT, Record<string, any>>((state) =>
    selectActiveFormValues(state),
  );
  const otherConceptListFields = useVisibleConceptListFields().filter(
    (field) => field.name !== targetFieldname,
  );

  // Need to have min 2 fields to copy from one to another
  if (otherConceptListFields.length < 1) return false;

  const fieldHasFilledConcept = (field: ConceptListField) =>
    !!values[field.name] &&
    values[field.name].some(
      (value /* TODO: type it FormConceptGroup value */) =>
        value.concepts.some(exists),
    );

  return otherConceptListFields.some(fieldHasFilledConcept);
};

export const useFormLabelByType = (formType: string) => {
  const availableForms = useSelector<StateT, { [formName: string]: Form }>(
    (state) => selectAvailableForms(state),
  );
  const activeLang = useActiveLang();

  return availableForms[formType]
    ? availableForms[formType].title[activeLang]
    : formType;
};
