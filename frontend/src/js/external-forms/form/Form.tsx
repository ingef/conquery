import styled from "@emotion/styled";
import { StateT } from "app-types";
import { memo, useMemo } from "react";
import { useForm, useFormState } from "react-hook-form";
import { TFunction, useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { SelectOptionT } from "../../api/types";
import type { DatasetT } from "../../dataset/reducer";
import { useActiveLang } from "../../localization/useActiveLang";
import FormConfigSaver from "../FormConfigSaver";
import FormHeader from "../FormHeader";
import type {
  Form as FormType,
  FormField as FormFieldType,
} from "../config-types";
import { collectAllFormFields, isFormField, isOptionalField } from "../helper";
import {
  validateRequired,
  validateDateRange,
  validatePositive,
  validateConceptGroupFilled,
  validateDateRangeRequired,
} from "../validators";

import Field from "./Field";

const DEFAULT_VALUE_BY_TYPE = {
  STRING: "",
  NUMBER: null,
  CHECKBOX: false,
  CONCEPT_LIST: [],
  RESULT_GROUP: null,
  MULTI_RESULT_GROUP: [],
  SELECT: null,
  TABS: null,
  DATASET_SELECT: null,
  MULTI_SELECT: null,
  DATE_RANGE: {
    min: null,
    max: null,
  },
};

const DEFAULT_VALIDATION_BY_TYPE = {
  STRING: null,
  NUMBER: null,
  CHECKBOX: null,
  CONCEPT_LIST: null,
  RESULT_GROUP: null,
  MULTI_RESULT_GROUP: null,
  SELECT: null,
  TABS: null,
  DATASET_SELECT: null,
  MULTI_SELECT: null,
  DATE_RANGE: validateDateRange,
};

const SxFormHeader = styled(FormHeader)`
  margin: 5px 0 15px;
`;

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

function getInitialValue(
  field: FormFieldType,
  context: { availableDatasets: SelectOptionT[] },
) {
  if (field.type === "DATASET_SELECT" && context.availableDatasets.length > 0) {
    return context.availableDatasets[0].value;
  }

  return field.defaultValue || DEFAULT_VALUE_BY_TYPE[field.type];
}

function getErrorForField(t: TFunction, field: FormFieldType, value: any) {
  const defaultValidation = DEFAULT_VALIDATION_BY_TYPE[field.type];

  let error = defaultValidation ? defaultValidation(t, value) : null;

  if (!!field.validations && field.validations.length > 0) {
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

interface Props {
  config: FormType;
  availableDatasets: SelectOptionT[];
}

export interface DynamicFormValues {
  [fieldname: string]: unknown;
}

const Form = memo(({ config }: Props) => {
  // const { t } = useTranslation();
  const { register, control } = useForm<DynamicFormValues>();
  const availableDatasets = useSelector<StateT, DatasetT[]>(
    (state) => state.datasets.data,
  );
  const datasetOptions = useMemo(
    () =>
      availableDatasets.map((dataset) => ({
        label: dataset.label,
        value: dataset.id,
      })),
    [availableDatasets],
  );
  // const allFields = collectAllFormFields(config.fields);

  const activeLang = useActiveLang();

  return (
    <div>
      {config.description && config.description[activeLang] && (
        <SxFormHeader description={config.description[activeLang]!} />
      )}
      <FormConfigSaver />
      {config.fields.map((field, i) => {
        const key = isFormField(field) ? field.name : field.type + i;
        const optional = isOptionalField(field);

        return (
          <Field
            key={key}
            formType={config.type}
            getFieldValue={() => null}
            // getFieldValue={() =>
            //   isFormField(field) ? getFieldValue(field.name) : null
            // }
            register={register}
            control={control}
            field={field}
            availableDatasets={datasetOptions}
            locale={activeLang}
            optional={optional}
          />
        );
      })}
    </div>
  );
});

export default Form;
