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
