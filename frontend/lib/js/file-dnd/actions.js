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
import {
  uploadConceptListModalOpen,
  selectConceptRootNodeAndResolveCodes
}                                       from '../upload-concept-list-modal/actions';

import {
  DROP_FILES_START,
  DROP_FILES_SUCCESS,
  DROP_FILES_ERROR
}                                       from "./actionTypes";
import type {
  DraggedFileType,
  ConceptFileType
}                                       from "./types";

export const loadFilesStart = () =>
  ({ type: DROP_FILES_START });
export const loadFilesSuccess = (res: any) =>
  defaultSuccess(DROP_FILES_SUCCESS, res);
export const loadFilesError = (err: any) =>
  defaultError(DROP_FILES_ERROR, err);

export const dropConceptFile = (item: DraggedFileType, conceptFile: ConceptFileType) => {
  return (dispatch: Dispatch) => {
    dispatch(loadFilesStart());

    // Ignore all dropped files except the first
    const file = item[0];

    // default callback
    conceptFile['callback'] = conceptFile.callback || selectConceptRootNodeAndResolveCodes;

    if (!checkFileType(file))
      return dispatch(loadFilesError(new Error("Invalid concept list file")));

    return readFileAsText(file).then(
      r => {
        const values = cleanFileContent(r);
        conceptFile.parameters['values'] = values;
        conceptFile.parameters['fileName'] = file.name;

        if (values.length)
          return dispatch([
            loadFilesSuccess(),
            uploadConceptListModalOpen(conceptFile)
          ]);

        return dispatch(loadFilesError(new Error('An empty file was dropped')));
      },
      e => dispatch(loadFilesError(e))
    );
  }
};

export const dropOrConceptFile = (item: DraggedFileType, andIdx: number) =>
  dropConceptFile(item, { parameters: { andIdx } });
