// @flow

import { uploadConceptListModalOpen } from '../upload-concept-list-modal/actions';
import { createActionTypes }          from './actionTypes';

export const createQueryNodeEditorActions = (type: string): Object => {
  const {
    SET_DETAILS_VIEW_ACTIVE,
    SET_INPUT_TABLE_VIEW_ACTIVE,
    SET_FOCUSED_INPUT,
    TOGGLE_EDIT_LABEL,
    RESET,
  } = createActionTypes(type);

  const setDetailsViewActive = () => ({type: SET_DETAILS_VIEW_ACTIVE});
  const setInputTableViewActive = (tableIdx) => ({type: SET_INPUT_TABLE_VIEW_ACTIVE, tableIdx});
  const setFocusedInput = (filterIdx) => ({type: SET_FOCUSED_INPUT, filterIdx});
  const toggleEditLabel = () => ({type: TOGGLE_EDIT_LABEL});
  const reset = () => ({type: RESET});
  const onDropFiles = (filterIdx, files) => dropConceptFile(files, {filterIdx});

  return {
    setDetailsViewActive,
    setInputTableViewActive,
    setFocusedInput,
    toggleEditLabel,
    reset,
    onDropFiles
  };
};

export const dropConceptFile = (item: DraggedFileType, queryContext: Object = {}) => {
  return (dispatch: Dispatch) => {
    // dispatch(loadFilesStart());

    // Ignore all dropped files except the first
    const file = item[0];

    // if (!validateConceptListFile(file))
    //   return dispatch(loadFilesError(new Error("Invalid concept list file")));

    return readConceptListFile(file).then(
      r => {
        const conceptCodes = parseConceptListFile(r);

        if (conceptCodes.length)
          return dispatch([
            // loadFilesSuccess(),
            uploadConceptListModalOpen({
              fileName: file.name,
              conceptCodes,
              queryContext
            })
          ]);

        // return dispatch(loadFilesError(new Error('An empty file was dropped')));
      });
  }
};

const readConceptListFile = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader();

  reader.onload = (evt) => resolve(evt.target.result);
  reader.onerror = (err) => reject(err);

  reader.readAsText(file);
});

const parseConceptListFile = (fileContents) => {
  return fileContents.split('\n')
    .map(row => row.trim())
    .filter(row => row.length > 0);
};