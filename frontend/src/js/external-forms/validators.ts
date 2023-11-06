import { TFunction } from "i18next";

import { isEmpty } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";
import { isValidSelect } from "../model/select";

import {
  CheckboxField,
  ConceptListField,
  Field,
  FormField,
} from "./config-types";
import { FormConceptGroupT } from "./form-concept-group/formConceptGroupState";

export const validateRequired = (
  t: TFunction,
  value: unknown,
): string | null => {
  return isEmpty(value) ? t("externalForms.formValidation.isRequired") : null;
};

export const validatePositive = (t: TFunction, value: number) => {
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

const validateRestrictedSelects = (
  t: TFunction,
  value: FormConceptGroupT[],
  field: ConceptListField,
) => {
  if (!value || value.length === 0) return null;

  const { allowlistedSelects, blocklistedSelects } = field;

  const hasAllowlistedSelects = (allowlistedSelects?.length || 0) > 0;
  const hasBlocklistedSelects = (blocklistedSelects?.length || 0) > 0;

  if (hasAllowlistedSelects || hasBlocklistedSelects) {
    const validSelects = value
      .flatMap((v) => v.concepts)
      .filter(exists)
      .flatMap((c) => {
        const tableSelects = c.tables.flatMap((t) => t.selects);

        return [...c.selects, ...tableSelects].filter(
          isValidSelect({ allowlistedSelects, blocklistedSelects }),
        );
      });

    if (validSelects.length === 0) {
      return t("externalForms.formValidation.validSelectRequired");
    }
  }

  return null;
};

// TODO: Refactor using generics to try and tie the `field` to its `value`
const DEFAULT_VALIDATION_BY_TYPE: Record<
  FormField["type"],
  null | ((t: TFunction, value: unknown, field: unknown) => string | null)
> = {
  STRING: null,
  TEXTAREA: null,
  NUMBER: null,
  CHECKBOX: null,
  // @ts-ignore TODO: Refactor using generics to try and tie the `field` to its `value`
  CONCEPT_LIST: validateRestrictedSelects,
  RESULT_GROUP: null,
  SELECT: null,
  TABS: null,
  DATASET_SELECT: null,
  GROUP: null,
  // MULTI_SELECT: null,
  // @ts-ignore TODO: Refactor using generics to try and tie the `field` to its `value`
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

function getConfigurableValidations(fieldType: string) {
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

  let error = defaultValidation ? defaultValidation(t, value, field) : null;

  if (
    isFieldWithValidations(field) &&
    !!field.validations &&
    field.validations.length > 0
  ) {
    for (const validation of field.validations) {
      const validateFn = getConfigurableValidations(field.type)[validation];

      if (validateFn) {
        // @ts-ignore TODO: try and improve these types
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
