import type { SelectOptionT } from "../api/types";
import type { Language } from "../localization/useActiveLang";

import type { FormField, GeneralField, Group } from "./config-types";

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
    if (field.type === "GROUP") {
      return [field, ...collectAllFormFields(field.fields)];
    } else if (field.type === "TABS") {
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
  field: Exclude<FormField, Group>,
  context: { availableDatasets: SelectOptionT[]; activeLang: Language },
):
  | string
  | number
  | boolean
  | undefined
  | Array<unknown>
  | { min: null; max: null }
  | SelectOptionT {
  switch (field.type) {
    case "DATASET_SELECT":
      if (context.availableDatasets.length > 0) {
        return context.availableDatasets[0];
      } else {
        return undefined;
      }
    case "SELECT":
      if (field.options.length > 0) {
        const options = field.options.map((option) => ({
          label: option.label[context.activeLang] || "",
          value: option.value,
        }));
        return options.find((option) => option.value === field.defaultValue);
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
