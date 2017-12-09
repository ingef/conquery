import { createQueryNodeModalActions } from '../query-node-modal';
import { uploadConceptListModalOpen }  from '../upload-concept-list-modal/actions';

import {
  defaultSuccess,
  defaultError
} from '../common/actions';

import {
  DROP_AND_NODE,
  LOAD_FILES_START,
  LOAD_FILES_SUCCESS,
  LOAD_FILES_ERROR,
  DROP_OR_NODE,
  DELETE_NODE,
  DELETE_GROUP,
  TOGGLE_EXCLUDE_GROUP,
  LOAD_QUERY,
  CLEAR_QUERY,
  EXPAND_PREVIOUS_QUERY,
  SHOW_CONCEPT_LIST_DETAILS,
  HIDE_CONCEPT_LIST_DETAILS,
} from './actionTypes';


export const dropAndNode = (item, dateRange) => ({
  type: DROP_AND_NODE,
  payload: { item, dateRange }
});

export const loadFilesStart = () =>
  ({ type: LOAD_FILES_START });
export const loadFilesSuccess = (res) =>
  defaultSuccess(LOAD_FILES_SUCCESS, res);
export const loadFilesError = (err) =>
  defaultError(LOAD_FILES_ERROR, err);

const validateConceptListFile = (file) => {
  return file.type === "text/plain";
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

export const dropConceptListFile = (item, queryContext = {}) => {
  return (dispatch) => {
    dispatch(loadFilesStart());

    // Ignore all dropped files except the first
    const file = item.files[0];

    if (!validateConceptListFile(file))
      return dispatch(loadFilesError(new Error("Invalid concept list file")));

    return readConceptListFile(file).then(
      r => {
        const conceptCodes = parseConceptListFile(r);

        if (conceptCodes.length)
          return dispatch([
            loadFilesSuccess(),
            uploadConceptListModalOpen({
              fileName: file.name,
              conceptCodes,
              queryContext
            })
          ]);

        return dispatch(loadFilesError(new Error('An empty file was dropped')));
      },
      e => dispatch(loadFilesError(e))
    );
  }
};

export const dropOrConceptListFile = (item, andIdx) => dropConceptListFile(item, { andIdx });

export const dropOrNode = (item, andIdx) => ({
  type: DROP_OR_NODE,
  payload: { item, andIdx }
});

export const deleteNode = (andIdx, orIdx) => ({
  type: DELETE_NODE,
  payload: { andIdx, orIdx }
});

export const deleteGroup = (andIdx, orIdx) => ({
  type: DELETE_GROUP,
  payload: { andIdx, orIdx }
});

export const toggleExcludeGroup = (andIdx) => ({
  type: TOGGLE_EXCLUDE_GROUP,
  payload: { andIdx }
});

export const loadQuery = (query) => ({
  type: LOAD_QUERY,
  payload: { query }
});

export const clearQuery = () => ({ type: CLEAR_QUERY });

export const expandPreviousQuery = (rootConcepts, groups) => ({
  type: EXPAND_PREVIOUS_QUERY,
  payload: { rootConcepts, groups }
});

export const showConceptListDetails = (andIdx, orIdx) =>
  ({ type: SHOW_CONCEPT_LIST_DETAILS, payload: { andIdx, orIdx } });

export const hideConceptListDetails = () => ({ type: HIDE_CONCEPT_LIST_DETAILS });

export const {
  setStandardNode,
  clearStandardNode,
  toggleStandardTable,
  setStandardFilterValue,
  switchStandardFilterMode,
  resetStandardAllFilters,
  toggleStandardTimestamps,
  loadStandardFilterSuggestions,
} = createQueryNodeModalActions('standard');
