import { toUpperCaseUnderscore } from "../../common/helpers";

export const createActionTypes = (formType: string, fieldName: string) => {
  const uppercasedFieldName = toUpperCaseUnderscore(fieldName);
  return {
    LOAD_FILTER_SUGGESTIONS_START: `form-suggestions/LOAD_${formType}_${uppercasedFieldName}_FILTER_SUGGESTIONS_START`,
    LOAD_FILTER_SUGGESTIONS_SUCCESS: `form-suggestions/LOAD_${formType}_${uppercasedFieldName}_FILTER_SUGGESTIONS_SUCCESS`,
    LOAD_FILTER_SUGGESTIONS_ERROR: `form-suggestions/LOAD_${formType}_${uppercasedFieldName}_FILTER_SUGGESTIONS_ERROR`
  };
};
