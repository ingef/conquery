import { TFunction } from "react-i18next";

import { isEmpty } from "../common/helpers";

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
