import { useWatch } from "react-hook-form";
import { useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";
import { useActiveLang } from "../localization/useActiveLang";

import { ConceptListField, Form, GeneralField } from "./config-types";
import type { FormConceptGroupT } from "./form-concept-group/formConceptGroupState";

export const selectAvailableForms = (state: StateT) =>
  state.externalForms ? state.externalForms.availableForms : {};

export const selectActiveFormType = (state: StateT) =>
  state.externalForms ? state.externalForms.activeForm : null;

export const selectFormConfig = (state: StateT): Form | null => {
  const availableForms = selectAvailableForms(state);
  const activeFormType = selectActiveFormType(state);

  return (activeFormType && availableForms[activeFormType]) || null;
};

export const selectQueryRunner = (state: StateT) =>
  state.externalForms ? state.externalForms.queryRunner : null;

export const selectRunningQuery = (state: StateT) => {
  const queryRunner = selectQueryRunner(state);

  return queryRunner ? queryRunner.runningQuery : null;
};

function getVisibleConceptListFields(
  fields: GeneralField[],
  values: Record<string, any>,
): ConceptListField[] {
  return fields
    .flatMap((field) => {
      switch (field.type) {
        case "GROUP":
          return field.fields;
        case "TABS":
          const activeTabName = values[field.name];
          const activeTab = field.tabs.find(
            (tab) => tab.name === activeTabName,
          );

          return activeTab
            ? getVisibleConceptListFields(activeTab.fields, values)
            : [];
        default:
          return [field];
      }
    })
    .filter(
      (field): field is ConceptListField => field.type === "CONCEPT_LIST",
    );
}

export const useVisibleConceptListFields = () => {
  const config = useSelector<StateT, Form | null>((state) =>
    selectFormConfig(state),
  );
  const values = useWatch({});

  if (!config) return [];

  return getVisibleConceptListFields(config.fields, values);
};

export const useAllowExtendedCopying = (
  targetFieldname: string,
  visibleConceptListFields: ConceptListField[],
) => {
  const values = useWatch({});
  const otherConceptListFields = visibleConceptListFields.filter(
    (field) => field.name !== targetFieldname,
  );

  // Need to have min 2 fields to copy from one to another
  if (otherConceptListFields.length < 1) return false;

  const targetField = visibleConceptListFields.find(
    (field) => field.name === targetFieldname,
  );
  if (targetField?.hideCopyButton) return false;

  const fieldHasFilledConcept = (field: ConceptListField) => {
    return (
      !!values[field.name] &&
      values[field.name].some((value: FormConceptGroupT) =>
        value.concepts.some(exists),
      )
    );
  };

  return otherConceptListFields.some(fieldHasFilledConcept);
};

export const useFormLabelByType = (formType: string | null) => {
  const availableForms = useSelector<StateT, { [formName: string]: Form }>(
    (state) => selectAvailableForms(state),
  );
  const activeLang = useActiveLang();

  if (!formType) return null;

  return availableForms[formType]
    ? availableForms[formType].title[activeLang]
    : formType;
};
