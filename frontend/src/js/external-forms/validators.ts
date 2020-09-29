import { T } from "../localization";
import { isEmpty } from "../common/helpers";

export const validateRequired = (value: any): string | null => {
  return isEmpty(value)
    ? T.translate("externalForms.formValidation.isRequired")
    : null;
};

export const validatePositive = (value: any) => {
  return value > 0
    ? null
    : T.translate("externalForms.formValidation.mustBePositiveNumber");
};

export const validateDateRange = (value: { min: string; max: string }) => {
  // May be empty
  if (!value || (!value.min && !value.max)) return null;

  // But if not, must be set fully and correctly
  if (!value.min || !value.max)
    return T.translate("externalForms.formValidation.isRequired");
  if (value.max < value.min)
    return T.translate("externalForms.formValidation.invalidDateRange");

  return null;
};

export const validateDateRangeRequired = (
  value: {
    min: string;
    max: string;
  } | null
): string | null => {
  if (!value || !value.min || !value.max)
    return T.translate("externalForms.formValidation.isRequired");

  return validateDateRange(value);
};

export const validateConceptGroupFilled = (
  group: { concepts: [] }[]
): string | null => {
  if (!group || group.length === 0)
    return T.translate("externalForms.formValidation.isRequired");

  return group.some(
    (e) => e.concepts.length === 0 || e.concepts.some((c) => !c)
  )
    ? T.translate("externalForms.formValidation.isRequired")
    : null;
};
