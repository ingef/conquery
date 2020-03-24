import {
  LOAD_CONFIG_START,
  LOAD_CONFIG_ERROR,
  LOAD_CONFIG_SUCCESS
} from "./actionTypes";

import type { GetFrontendConfigResponseT } from "../api/types";

export type StartupStateT = {
  loading: boolean,
  error: string | null,
  config: GetFrontendConfigResponseT
};

const initialState: StartupStateT = {
  loading: false,
  error: null,
  config: {
    version: "No version loaded",
    currency: {
      prefix: "€",
      thousandSeparator: ".",
      decimalSeparator: ",",
      decimalScale: 2
    }
  }
};

const startup = (
  state: StartupStateT = initialState,
  action: Object
): StartupStateT => {
  switch (action.type) {
    case LOAD_CONFIG_START:
      return {
        ...state,
        loading: true
      };
    case LOAD_CONFIG_ERROR:
      return {
        ...state,
        loading: false,
        error: action.payload.message
      };
    case LOAD_CONFIG_SUCCESS:
      return {
        ...state,
        loading: false,
        config: action.payload.data
      };
    default:
      return state;
  }
};

export default startup;
