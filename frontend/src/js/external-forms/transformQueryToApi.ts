import { transformElementsToApi } from "../api/apiHelper";
import type { SelectOptionT } from "../api/types";
import type { DateStringMinMax } from "../common/helpers/dateHelper";
import { exists } from "../common/helpers/exists";
import type { DragItemQuery } from "../standard-query-editor/types";

import type { Form, GeneralField } from "./config-types";
import type { FormConceptGroupT } from "./form-concept-group/formConceptGroupState";
import type { DynamicFormValues } from "./form/Form";
import {
  collectAllFormFields,
  getRawFieldname,
  getUniqueFieldname,
  isFormField,
} from "./helper";

function transformElementGroupsToApi(elementGroups: FormConceptGroupT[]) {
  const elementGroupsWithAtLeastOneElement = elementGroups
    .map(({ concepts, ...rest }) => ({
      concepts: concepts.filter(exists),
      ...rest,
    }))
    .filter(({ concepts }) => concepts.length > 0);

  return elementGroupsWithAtLeastOneElement.map(
    ({ concepts, connector, ...rest }) =>
      concepts.length > 1
        ? {
            type: connector,
            children: transformElementsToApi(concepts),
            ...rest,
          }
        : { ...transformElementsToApi(concepts)[0], ...rest },
  );
}

function transformFieldToApiEntries(
  configType: string,
  fieldConfig: GeneralField,
  formValues: DynamicFormValues,
): [string, any][] {
  if (!isFormField(fieldConfig)) {
    return [];
  }
  const formValue =
    fieldConfig.type === "GROUP"
      ? null
      : formValues[getUniqueFieldname(configType, fieldConfig)];

  switch (fieldConfig.type) {
    case "CHECKBOX":
      return [[fieldConfig.name, formValue || false]];
    case "TEXTAREA":
    case "STRING":
    case "NUMBER":
      return [[fieldConfig.name, formValue ?? null]];
    case "DATASET_SELECT":
    case "SELECT":
      return [
        [
          fieldConfig.name,
          formValue ? (formValue as SelectOptionT).value : null,
        ],
      ];
    case "RESULT_GROUP":
      // A RESULT_GROUP field may allow null / be optional
      return [
        [fieldConfig.name, formValue ? (formValue as DragItemQuery).id : null],
      ];
    case "DATE_RANGE":
      return [
        [
          fieldConfig.name,
          {
            min: (formValue as DateStringMinMax).min,
            max: (formValue as DateStringMinMax).max,
          },
        ],
      ];
    case "CONCEPT_LIST":
      return [
        [
          fieldConfig.name,
          transformElementGroupsToApi(formValue as FormConceptGroupT[]),
        ],
      ];
    case "GROUP":
      return fieldConfig.fields.flatMap((f) =>
        transformFieldToApiEntries(configType, f, formValues),
      );
    case "TABS":
      const selectedTab = fieldConfig.tabs.find(
        (tab) => tab.name === formValue,
      );

      if (!selectedTab) {
        throw new Error(
          `No tab selected for ${fieldConfig.name}, this shouldn't happen`,
        );
      }

      return [
        [
          fieldConfig.name,
          {
            value: formValue,
            // Only include field values from the selected tab
            ...transformFieldsToApi(configType, selectedTab.fields, formValues),
          },
        ],
      ];
  }
}

function transformFieldsToApi(
  configType: string,
  fields: GeneralField[],
  formValues: DynamicFormValues,
): DynamicFormValues {
  return Object.fromEntries(
    fields.flatMap((field) =>
      transformFieldToApiEntries(configType, field, formValues),
    ),
  );
}

const transformQueryToApi = (
  formConfig: Form,
  formValues: DynamicFormValues,
) => {
  const formFields = collectAllFormFields(formConfig.fields);
  const formSpecificValuesToSave = Object.fromEntries(
    Object.entries(formValues)
      .filter(([k]) =>
        formFields.some(
          (f) =>
            f.type !== "GROUP" && getUniqueFieldname(formConfig.type, f) === k,
        ),
      )
      .map(([k, v]) => [getRawFieldname(k), v]),
  );

  return {
    type: formConfig.type,
    values: formSpecificValuesToSave,
    ...transformFieldsToApi(formConfig.type, formConfig.fields, formValues),
  };
};

export default transformQueryToApi;
