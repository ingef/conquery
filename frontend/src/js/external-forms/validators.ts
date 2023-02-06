import { TFunction } from "react-i18next";

import { isEmpty } from "../common/helpers/commonHelper";

import { CheckboxField, Field, FormField } from "./config-types";

export const validateRequired = (t: TFunction, value: any): string | null => {
  return isEmpty(value) ? t("externalForms.formValidation.isRequired") : null;
};

export const validatePositive = (t: TFunction, value: any) => {
  return isEmpty(value) || value > 0
    ? null
    : t("externalForms.formValidation.mustBePositiveNumber");
};

export const validateDateRange = (
  t: TFunction,
  value: { min: string; max: string },
) => {
  // May be empty
  if (!value || (!value.min && !value.max)) return null;

  // But if not, must be set fully and correctly
  if (!value.min || !value.max)
    return t("externalForms.formValidation.isRequired");
  if (value.max < value.min)
    return t("externalForms.formValidation.invalidDateRange");

  return null;
};

export const validateDateRangeRequired = (
  t: TFunction,
  value: {
    min: string;
    max: string;
  } | null,
): string | null => {
  if (!value || !value.min || !value.max)
    return t("externalForms.formValidation.isRequired");

  return validateDateRange(t, value);
};

export const validateConceptGroupFilled = (
  t: TFunction,
  group: { concepts: [] }[],
): string | null => {
  if (!group || group.length === 0)
    return t("externalForms.formValidation.isRequired");

  return group.some(
    (e) => e.concepts.length === 0 || e.concepts.some((c) => !c),
  )
    ? t("externalForms.formValidation.isRequired")
    : null;
};

const DEFAULT_VALIDATION_BY_TYPE: Record<
  FormField["type"],
  null | ((t: TFunction, value: any) => string | null)
> = {
  STRING: null,
  NUMBER: null,
  CHECKBOX: null,
  CONCEPT_LIST: null,
  RESULT_GROUP: null,
  SELECT: null,
  TABS: null,
  DATASET_SELECT: null,
  GROUP: null,
  // MULTI_SELECT: null,
  DATE_RANGE: validateDateRange,
};

function getNotEmptyValidation(fieldType: string) {
  switch (fieldType) {
    case "CONCEPT_LIST":
      return validateConceptGroupFilled;
    case "DATE_RANGE":
      return validateDateRangeRequired;
    default:
      return validateRequired;
  }
}

function getPossibleValidations(fieldType: string) {
  return {
    NOT_EMPTY: getNotEmptyValidation(fieldType),
    GREATER_THAN_ZERO: validatePositive,
  };
}

const isFieldWithValidations = (
  field: FormField,
): field is Exclude<Field, CheckboxField> => {
  return (
    field.type !== "TABS" && field.type !== "GROUP" && field.type !== "CHECKBOX"
  );
};

export function getErrorForField(
  t: TFunction,
  field: FormField,
  value: unknown,
) {
  const defaultValidation = DEFAULT_VALIDATION_BY_TYPE[field.type];

  let error = defaultValidation ? defaultValidation(t, value) : null;

  if (
    isFieldWithValidations(field) &&
    !!field.validations &&
    field.validations.length > 0
  ) {
    for (let validation of field.validations) {
      const validateFn = getPossibleValidations(field.type)[validation];

      if (validateFn) {
        error = error || validateFn(t, value);
      } else {
        console.error(
          "Validation configured that is not supported: ",
          validation,
          "for field",
          field.name,
        );
      }
    }
  }

  return error;
}
