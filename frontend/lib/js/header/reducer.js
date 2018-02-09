import {
  LOAD_VERSION_START,
  LOAD_VERSION_SUCCESS,
  LOAD_VERSION_ERROR
} from './actionTypes'

export type StateType = {
  loading: boolean,
  development: boolean,
  version: string
};

const initialState: StateType = {};

export const version = (state: StateType = initialState, action: Object): StateType => {
  switch (action.type) {
    case LOAD_VERSION_START:
      return { ...state, loading: true };
    case LOAD_VERSION_SUCCESS:
      return {
        ...state,
        loading: false,
        isDevelopment: action.payload.data.isDevelopment,
        version: action.payload.data.version
      };
      case LOAD_VERSION_ERROR:
          return { ...state, loading: false, error: action.payload.message };
    default:
      return state;
  }
};
