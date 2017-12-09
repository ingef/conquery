// @flow

import {
  OPEN_UPLOAD_MODAL,
  CLOSE_UPLOAD_MODAL,
  UPLOAD_FILE_START,
  UPLOAD_FILE_SUCCESS,
  UPLOAD_FILE_ERROR,
} from './actionTypes';

export type UploadReportType = {
  successful: number,
  unsuccessful: number
};

type StateType = {
  isModalOpen: boolean,
  isLoading: boolean,
  success: ?UploadReportType,
  error: ?(UploadReportType & { message: string }),
};

const initialState = {
  isModalOpen: false,
  isLoading: false,
  success: null,
  error: null,
};

const queryResultUpload = (state: StateType = initialState, action: Object): StateType => {
  switch (action.type) {
    case OPEN_UPLOAD_MODAL:
      return { ...state, isModalOpen: true, loading: false, success: null, error: null };
    case CLOSE_UPLOAD_MODAL:
      return initialState;
    case UPLOAD_FILE_START:
      return { ...state, loading: true, success: null, error: null };
    case UPLOAD_FILE_SUCCESS:
      return { ...state, loading: false, success: action.payload.data };
    case UPLOAD_FILE_ERROR:
      return { ...state, loading: false, error: action.payload };
    default:
      return state;
  }
};

export default queryResultUpload;
