import { LOAD_CONFIG_START, LOAD_CONFIG_SUCCESS, LOAD_CONFIG_ERROR } from "./actionTypes";

export type StateType = {
    loading: boolean,
    config: Object
}

const initialState: StateType = {};

export const config = (state: StateType = initialState, action: Object): StateType => {
  switch (action.type) {
    case LOAD_CONFIG_START:
      return { ...state, loading: true };
    case LOAD_CONFIG_SUCCESS:
      return {
        ...state,
        loading: false,
        config: action.payload.data.config
      };
    case LOAD_CONFIG_ERROR:
      return { ...state, loading: false, error: action.payload.message };
    default:
      return state;
  }
}
