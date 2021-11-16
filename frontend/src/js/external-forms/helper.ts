import type { SelectOptionT } from "../api/types";

import type { FormField, GeneralField } from "./config-types";

const nonFormFieldTypes = new Set(["HEADLINE", "DESCRIPTION"]);

export const isOptionalField = (field: GeneralField) => {
  return (
    isFormField(field) &&
    (!("validations" in field) ||
      ("validations" in field &&
        (!field.validations || !field.validations.includes("NOT_EMPTY"))))
  );
};

export const isFormField = (field: GeneralField): field is FormField => {
  return !nonFormFieldTypes.has(field.type);
};

export function collectAllFormFields(fields: GeneralField[]): FormField[] {
  return fields.filter(isFormField).flatMap((field) => {
    if (field.type === "TABS") {
      return [
        field,
        ...field.tabs.flatMap((tab) => collectAllFormFields(tab.fields)),
      ];
    } else {
      return field;
    }
  });
}

export function getInitialValue(
  field: FormField,
  context: { availableDatasets: SelectOptionT[] },
):
  | string
  | number
  | boolean
  | undefined
  | Array<unknown>
  | { min: null; max: null } {
  switch (field.type) {
    case "DATASET_SELECT":
      if (context.availableDatasets.length > 0) {
        return context.availableDatasets[0].value;
      } else {
        return undefined;
      }
    case "RESULT_GROUP":
      return undefined;
    case "MULTI_RESULT_GROUP":
    case "CONCEPT_LIST":
      return [];
    case "DATE_RANGE":
      return {
        min: null,
        max: null,
      };
    default:
      return field.defaultValue || undefined;
  }
}
