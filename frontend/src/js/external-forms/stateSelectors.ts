import { useWatch } from "react-hook-form";
import { useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";
import { useActiveLang } from "../localization/useActiveLang";

import { ConceptListField, Form, GeneralField } from "./config-types";
import type { FormConceptGroupT } from "./form-concept-group/formConceptGroupState";
import { getUniqueFieldname } from "./helper";

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
  config: Form,
  fields: GeneralField[],
  values: Record<string, any>,
): ConceptListField[] {
  return fields
    .flatMap((field) => {
      switch (field.type) {
        case "GROUP":
          return field.fields;
        case "TABS":
          const activeTabName = values[getUniqueFieldname(config.type, field)];
          const activeTab = field.tabs.find(
            (tab) => tab.name === activeTabName,
          );

          return activeTab
            ? getVisibleConceptListFields(config, activeTab.fields, values)
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

  return getVisibleConceptListFields(config, config.fields, values);
};

export const useAllowExtendedCopying = (
  targetFieldname: string,
  visibleConceptListFields: ConceptListField[],
) => {
  const config = useSelector<StateT, Form | null>((state) =>
    selectFormConfig(state),
  );
  const values = useWatch({});
  const otherConceptListFields = visibleConceptListFields.filter(
    (field) => field.name !== targetFieldname,
  );

  // Need to have min 2 fields to copy from one to another
  if (otherConceptListFields.length < 1) return false;

  const fieldHasFilledConcept = (field: ConceptListField) => {
    if (!config) return false;

    const uniqueFieldname = getUniqueFieldname(config.type, field);

    return (
      !!values[uniqueFieldname] &&
      values[uniqueFieldname].some((value: FormConceptGroupT) =>
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
