import { toUpperCaseUnderscore } from "../../common/helpers";
import { createQueryNodeEditorReducer } from "../../query-node-editor/reducer";

export const createFormQueryNodeEditorReducer = (
  formType: string,
  fieldName: string,
) =>
  createQueryNodeEditorReducer(
    `${formType}_${toUpperCaseUnderscore(fieldName)}`,
  );
