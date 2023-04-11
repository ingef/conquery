import type { SelectOptionT } from "../api/types";
import type { Language } from "../localization/useActiveLang";

import type { FormField, GeneralField, Group } from "./config-types";

const nonFormFieldTypes = new Set(["HEADLINE", "DESCRIPTION"]);

// Different forms may have fields with the same name.
// We want to remember values of fields of form A when switching to form B,
// so users may come back to form A and see their previous values.
// So in order to avoid field name clashes, we need unique field names
export const getUniqueFieldname = (formType: string, rawFieldName: string) => {
  return `${formType}--${rawFieldName}`;
};
export const getRawFieldname = (uniqueFieldname: string) => {
  return uniqueFieldname.split("--").at(-1);
};

export const getFieldKey = (
  formType: string,
  field: GeneralField,
  idx: number,
) => {
  return isFormField(field) && field.type !== "GROUP"
    ? formType + field.name
    : formType + field.type + idx;
};

export const getH1Index = (fields: GeneralField[], field: GeneralField) => {
  if (
    field.type !== "HEADLINE" ||
    !field.style?.size ||
    field.style.size !== "h1"
  ) {
    return;
  }

  const h1Fields = fields.filter(
    (f) => f.type === "HEADLINE" && f.style?.size === "h1",
  );

  return h1Fields.indexOf(field);
};

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
