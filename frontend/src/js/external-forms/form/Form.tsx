import styled from "@emotion/styled";
import { StateT } from "app-types";
import { memo, useMemo, useCallback } from "react";
import { TFunction, useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { reduxForm, formValueSelector } from "redux-form";

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
import { selectReduxFormState } from "../stateSelectors";
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

interface ConfiguredFormPropsType {
  config: FormType;
}

interface Props {
  config: FormType;
  availableDatasets: SelectOptionT[];
}

const Form = memo(({ config, availableDatasets }: Props) => {
  // TODO: THIS REALLY ISN'T IDEAL,
  // AS THE WHOLE FORM HAS TO RERENDER ON EVERY STATE CHANGE
  // WE WILL NEED TO MIGRATE AWAY FROM REDUX-FORM SOON
  const state = useSelector<StateT, StateT>((state) => state);
  const getFieldValue = useCallback(
    (fieldname: string) => {
      const fieldValueSelector = formValueSelector(
        config.type,
        selectReduxFormState,
      );

      return fieldValueSelector(state, fieldname);
    },
    [state, config.type],
  );

  const activeLang = useActiveLang();

  return (
    <form>
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
            getFieldValue={() =>
              isFormField(field) ? getFieldValue(field.name) : null
            }
            field={field}
            availableDatasets={availableDatasets}
            locale={activeLang}
            optional={optional}
          />
        );
      })}
    </form>
  );
});

// This is the generic form component that receives a form config
// and builds all fields from there.
//
// Note: The config contains the fields in a hierarchical structure,
//       because one of the fields is a "TAB", which contains subfields
//       depending on the tab, that is selected
const ConfiguredForm = ({ config, ...props }: ConfiguredFormPropsType) => {
  const { t } = useTranslation();
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
  const allFields = collectAllFormFields(config.fields);

  const ReduxFormConnectedForm = reduxForm({
    form: config.type,
    getFormState: selectReduxFormState,
    initialValues: Object.fromEntries(
      allFields.map((field) => [
        field.name,
        getInitialValue(field, { availableDatasets: datasetOptions }),
      ]),
    ),
    destroyOnUnmount: false,
    validate: (values) =>
      Object.keys(values).reduce((errors, name) => {
        const field = allFields.find((field) => field.name === name);

        // Note: For some reason, redux form understands, that:
        //       EVEN IF we add errors for ALL fields –
        //       including those fields that are not shown,
        //       because their tab is hidden – as long as those
        //       fields are not "rendered", the form seems to be valid
        //
        // => Otherwise, we'd have to check which tab is selected here,
        //    and which errors to add
        const error = getErrorForField(t, field, values[name]);

        if (error) {
          errors[name] = error;
        }

        return errors;
      }, {}),
  })(Form);

  return (
    <ReduxFormConnectedForm
      {...props}
      config={config}
      availableDatasets={datasetOptions}
    />
  );
};

export default ConfiguredForm;
