import { createQueryNodeEditorReducer } from "../../query-node-editor/reducer";
import { toUpperCaseUnderscore } from "../../common/helpers";

export const createFormQueryNodeEditorReducer = (
  formType: string,
  fieldName: string
) =>
  createQueryNodeEditorReducer(
    `${formType}_${toUpperCaseUnderscore(fieldName)}`
  );
