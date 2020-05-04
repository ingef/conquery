import { transformElementsToApi } from "../api/apiHelper";

import type { Form as FormType } from "./config-types";

function transformElementGroupsToApi(elementGroups) {
  return elementGroups.map(({ concepts, ...rest }) => ({
    type: "OR",
    children: transformElementsToApi(concepts),
    ...rest
  }));
}

function transformFieldToApi(fieldConfig, form) {
  const formValue = form[fieldConfig.name];

  switch (fieldConfig.type) {
    case "RESULT_GROUP":
      return formValue.id;
    case "MULTI_RESULT_GROUP":
      return formValue.map(group => group.id);
    case "DATE_RANGE":
      return {
        min: formValue.min,
        max: formValue.max
      };
    case "CONCEPT_LIST":
      return transformElementGroupsToApi(formValue);
    case "TABS":
      const selectedTab = fieldConfig.tabs.find(tab => tab.name === formValue);

      return {
        value: formValue,
        // Only include field values from the selected tab
        ...transformFieldsToApi(selectedTab.fields, form)
      };
    default:
      return formValue;
  }
}

function transformFieldsToApi(fields, form) {
  return fields.reduce((all, fieldConfig) => {
    all[fieldConfig.name] = transformFieldToApi(fieldConfig, form);

    return all;
  }, {});
}

function setFormSubtype(formConfig: FormType) {
  // Sets the subType member of the form request only if it was set in the form config.
  // It might be better to introduce a metadata field in the config-type.ts that is hidden in the representation but is send to the backend.
  return formConfig.subType ? { subType: formConfig.subType} : {};
}

const transformQueryToApi = (formConfig: FormType) => (form: Object) => {
  return {
    type: formConfig.type,
    ...setFormSubtype(formConfig),
    ...transformFieldsToApi(formConfig.fields, form)
  };
};

export default transformQueryToApi;
