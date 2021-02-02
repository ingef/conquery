import { transformElementsToApi } from "../api/apiHelper";
import { Form } from "./config-types";

function transformElementGroupsToApi(elementGroups) {
  return elementGroups.map(({ concepts, connector, ...rest }) => ({
    type: connector,
    children: transformElementsToApi(concepts),
    ...rest,
  }));
}

function transformFieldToApi(fieldConfig, form) {
  const formValue = form[fieldConfig.name];

  switch (fieldConfig.type) {
    case "RESULT_GROUP":
      return formValue.id;
    case "MULTI_RESULT_GROUP":
      return formValue.map((group) => group.id);
    case "DATE_RANGE":
      return {
        min: formValue.min,
        max: formValue.max,
      };
    case "CONCEPT_LIST":
      return transformElementGroupsToApi(formValue);
    case "TABS":
      const selectedTab = fieldConfig.tabs.find(
        (tab) => tab.name === formValue
      );

      return {
        value: formValue,
        // Only include field values from the selected tab
        ...transformFieldsToApi(selectedTab.fields, form),
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

const transformQueryToApi = (formConfig: Form) => (form: Object) => {
  return {
    type: formConfig.type,
    ...transformFieldsToApi(formConfig.fields, form),
  };
};

export default transformQueryToApi;
