import { getType } from "typesafe-actions";

import type { GetFrontendConfigResponseT } from "../api/types";
import type { Action } from "../app/actions";

import { loadConfig } from "./actions";

export type StartupStateT = {
  loading: boolean;
  error: string | null;
  config: GetFrontendConfigResponseT;
};

const initialState: StartupStateT = {
  loading: false,
  error: null,
  config: {
    version: "No version loaded",
    queryUpload: {
      ids: [],
    },
    currency: {
      prefix: "€",
      thousandSeparator: ".",
      decimalSeparator: ",",
      decimalScale: 2,
    },
  },
};

const startup = (
  state: StartupStateT = initialState,
  action: Action,
): StartupStateT => {
  switch (action.type) {
    case getType(loadConfig.request):
      return {
        ...state,
        loading: true,
      };
    case getType(loadConfig.failure):
      return {
        ...state,
        loading: false,
        error: action.payload.message || null,
      };
    case getType(loadConfig.success):
      return {
        ...state,
        loading: false,
        config: action.payload.data,
      };
    default:
      return state;
  }
};

export default startup;
