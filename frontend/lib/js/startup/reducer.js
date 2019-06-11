// @flow

import {
  LOAD_CONFIG_START,
  LOAD_CONFIG_ERROR,
  LOAD_CONFIG_SUCCESS
} from "./actionTypes";

import type { CurrencyConfigType } from "../common/types/backend";

export type ConfigType = {
  version: string,
  currency: CurrencyConfigType
};

export type StateType = {
  loading: boolean,
  error: ?string,
  config: ConfigType
};

const initialState: StateType = {
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
  state: StateType = initialState,
  action: Object
): StateType => {
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
