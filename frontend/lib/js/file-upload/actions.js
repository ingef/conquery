// @flow

import type { Dispatch }                from 'redux';

import {
  checkFileType,
  readFileAsText,
  cleanFileContent
}                                       from "../common/helpers";
import {
  defaultSuccess,
  defaultError
}                                       from "../common/actions";
import type { DateRangeType }           from "../common/types/backend";
import {
  uploadConceptListModalOpen
}                                       from "../upload-concept-list-modal/actions";
import {
  DROP_FILES_START,
  DROP_FILES_SUCCESS,
  DROP_FILES_ERROR
}                                       from "./actionTypes";
import type
{
  DraggedFileType,
  GenericFileType
}                                       from "./types";

const initialGenericFileType = (type?: GenericFileType) => ({
  parameters: type ? type.parameters : {},
  callback: uploadConceptListModalOpen
})

export const loadFilesStart = () =>
  ({ type: DROP_FILES_START });
export const loadFilesSuccess = (res: any) =>
  defaultSuccess(DROP_FILES_SUCCESS, res);
export const loadFilesError = (err: any) =>
  defaultError(DROP_FILES_ERROR, err);

export const dropFiles = (item: DraggedFileType, type?: GenericFileType) => {
  return (dispatch: Dispatch) => {
    dispatch(loadFilesStart());

    type = !type || !type.callback ? initialGenericFileType(type) : type;

    // Ignore all dropped files except the first
    const file = item[0] || item.files[0];

    if (!checkFileType(file))
      return dispatch(loadFilesError(new Error("Invalid concept list file")));

    return readFileAsText(file).then(
      r => {
        const values = cleanFileContent(r);
        type.parameters.values = values;
        type.parameters.fileName = file.name;

        if (values.length)
          return dispatch([
            loadFilesSuccess(r),
            type.callback(type)
          ]);

        return dispatch(loadFilesError(new Error('An empty file was dropped')));
      },
      e => dispatch(loadFilesError(e))
    );
  }
};

export const dropFilesAndIdx = (item: DraggedFileType, andIdx: number) =>
  dropFiles(item, { parameters: { andIdx }, callback: uploadConceptListModalOpen });

export const dropFilesDateRangeType = (item: DraggedFileType, dateRange: DateRangeType) =>
  dropFiles(item, { parameters: { dateRange }, callback: uploadConceptListModalOpen });
