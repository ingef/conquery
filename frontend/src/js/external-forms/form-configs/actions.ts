import { defaultError, defaultSuccess } from "../../common/actions";

import {
  LOAD_CONFIGS_ERROR,
  LOAD_CONFIGS_SUCCESS,
  PATCH_CONFIG_SUCCESS,
  DELETE_CONFIG_SUCCESS,
} from "./actionTypes";
import { FormConfigT } from "./reducer";

export const loadFormConfigsSuccess = (response: FormConfigT[]) =>
  defaultSuccess(LOAD_CONFIGS_SUCCESS, response);
export const loadFormConfigsError = (err: Error) =>
  defaultError(LOAD_CONFIGS_ERROR, err);

export const patchFormConfigSuccess = (
  configId: string,
  values: Partial<FormConfigT>,
) => defaultSuccess(PATCH_CONFIG_SUCCESS, null, { id: configId, values });

export const deleteFormConfigSuccess = (configId: string) =>
  defaultSuccess(DELETE_CONFIG_SUCCESS, null, { configId });
