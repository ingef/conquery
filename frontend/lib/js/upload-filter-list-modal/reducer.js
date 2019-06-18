// @flow

import type { PostFilterResolveResponseT } from "../api/types";

import {
  MODAL_CLOSE,
  RESOLVE_FILTER_VALUES_START,
  RESOLVE_FILTER_VALUES_SUCCESS,
  RESOLVE_FILTER_VALUES_ERROR
} from "./actionTypes";

export type StateType = {
  isModalOpen: boolean,
  loading: boolean,
  resolved: PostFilterResolveResponseT,
  error: ?Error
};

const initialState: StateType = {
  isModalOpen: false,
  loading: false,
  resolved: {},
  error: null
};

const uploadFilterListModal = (
  state: StateType = initialState,
  action: Object
) => {
  switch (action.type) {
    case RESOLVE_FILTER_VALUES_START:
      return {
        ...state,
        loading: true,
        error: null
      };
    case RESOLVE_FILTER_VALUES_SUCCESS:
      const { data } = action.payload;

      const hasUnresolvedCodes =
        data.unknownCodes && data.unknownCodes.length > 0;

      return {
        ...state,
        isModalOpen: hasUnresolvedCodes,
        loading: false,
        resolved: data
      };
    case RESOLVE_FILTER_VALUES_ERROR:
      return {
        ...state,
        loading: false,
        resolved: null,
        error: action.payload
      };
    case MODAL_CLOSE:
      return initialState;
    default:
      return state;
  }
};

export default uploadFilterListModal;
