// @flow

import { createQueryNodeEditorReducer } from "conquery/lib/js/query-node-editor";
import { toUpperCaseUnderscore } from "conquery/lib/js/common/helpers";

export const createFormQueryNodeEditorReducer = (
  formType: string,
  fieldName: string
) =>
  createQueryNodeEditorReducer(
    `${formType}_${toUpperCaseUnderscore(fieldName)}`
  );
