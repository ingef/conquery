import { toUpperCaseUnderscore } from "../common/helpers";

export const createActionTypes = (type: string) => {
  const uppercasedType = toUpperCaseUnderscore(type);

  return {
    SET_INPUT_TABLE_VIEW_ACTIVE: `query-node-editor/SET_${uppercasedType}_INPUT_TABLE_VIEW_ACTIVE`,
    SET_FOCUSED_INPUT: `query-node-editor/SET_${uppercasedType}_FOCUSED_INPUT`,
    UPDATE_NAME: `query-node-editor/UPDATE_${uppercasedType}_NAME`,
    RESET: `query-node-editor/RESET_${uppercasedType}`,
    RESOLVE_FILTER_VALUES: `query-node-editor/RESOLVE_${uppercasedType}_FILTER_VALUES`,
  };
};
