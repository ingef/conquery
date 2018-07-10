// @flow

import {
  LOAD_CONFIG_START,
  LOAD_CONFIG_ERROR,
  LOAD_CONFIG_SUCCESS
} from "./actionTypes";

export type StateType = {
  loading: boolean,
  error: ?string,
  config: Object
};

const config  = (state: StateType = {}, action: Object): StateType => {
  switch (action.type) {
    case LOAD_CONFIG_START:
      return {
        ...state,
        loading: true
      }
    case LOAD_CONFIG_ERROR:
      return {
        ...state,
        loading: false,
        error: action.payload.message
      }
    case LOAD_CONFIG_SUCCESS:
      return {
        ...state,
        loading: false,
        config: action.payload.data
      }
    default:
      return state;
  }
}

export default config;
