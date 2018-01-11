// @flow

import { type Dispatch }        from 'redux-thunk';

import api                      from '../api';

import {
  capitalize,
  toUpperCaseUnderscore,
}                               from '../common/helpers';

import { createActionTypes }    from './actionTypes';

export default function createQueryNodeModalActions(type: string): Object {
  const uppercaseUnderscoreType = toUpperCaseUnderscore(type)
  const capitalizedType = capitalize(type);

  const actionTypes = createActionTypes(uppercaseUnderscoreType);

  const setNode = (andIdx, orIdx) => ({
    type: actionTypes.SET_NODE,
    payload: { andIdx, orIdx }
  });

  const clearNode = () => ({ type: actionTypes.CLEAR_NODE });

  const toggleTable = (andIdx, orIdx, tableIdx, isExcluded) => ({
    type: actionTypes.TOGGLE_TABLE,
    payload: { andIdx, orIdx, tableIdx, isExcluded }
  });

  const setFilterValue = (andIdx, orIdx, tableIdx, filterIdx, value) => ({
    type: actionTypes.SET_FILTER_VALUE,
    payload: { andIdx, orIdx, tableIdx, filterIdx, value }
  });

  const resetAllFilters = (andIdx, orIdx) => ({
    type: actionTypes.RESET_ALL_FILTERS,
    payload: { andIdx, orIdx }
  });

  const switchFilterMode = (andIdx, orIdx, tableIdx, filterIdx, mode) => ({
    type: actionTypes.SWITCH_FILTER_MODE,
    payload: { andIdx, orIdx, tableIdx, filterIdx, mode }
  });

  const toggleTimestamps = (andIdx, orIdx, isExcluded) => ({
    type: actionTypes.TOGGLE_TIMESTAMPS,
    payload: { andIdx, orIdx, isExcluded }
  });

  const loadFilterSuggestionsStart = (andIdx, orIdx, tableIdx, conceptId, filterIdx, prefix) => ({
    type: actionTypes.LOAD_FILTER_SUGGESTIONS_START,
    payload: { andIdx, orIdx, tableIdx, conceptId, filterIdx, prefix }
  });

  const loadFilterSuggestionsSuccess = (suggestions, andIdx, orIdx, tableIdx, filterIdx) => ({
    type: actionTypes.LOAD_FILTER_SUGGESTIONS_SUCCESS,
    payload: {
      suggestions,
      andIdx,
      orIdx,
      tableIdx,
      filterIdx
    }
  });

  const loadFilterSuggestionsError = (error, andIdx, orIdx, tableIdx, filterIdx) => ({
    type: actionTypes.LOAD_FILTER_SUGGESTIONS_ERROR,
    payload: {
      message: error.message,
      ...error,
      andIdx,
      orIdx,
      tableIdx,
      filterIdx
    },
  });

  const loadFilterSuggestions =
    (datasetId, andIdx, orIdx, tableIdx, tableId, conceptId, filterIdx, filterId, prefix) => {
      return (dispatch: Dispatch) => {
        dispatch(loadFilterSuggestionsStart(andIdx, orIdx, tableIdx, conceptId, filterIdx, prefix));

        return api.postPrefixForSuggestions(datasetId, conceptId, tableId, filterId, prefix)
          .then(
            r => dispatch(loadFilterSuggestionsSuccess(r, andIdx, orIdx, tableIdx, filterIdx)),
            e => dispatch(loadFilterSuggestionsError(e, andIdx, orIdx, tableIdx, filterIdx))
          );
      };
  }

  const loadFormFilterSuggestionsStart =
    (formName, fieldName, andIdx, orIdx, tableIdx, conceptId, filterIdx, prefix) => ({
      type: actionTypes.LOAD_FILTER_SUGGESTIONS_START,
      payload: { formName, fieldName, andIdx, orIdx, tableIdx, conceptId, filterIdx, prefix }
  });

  const loadFormFilterSuggestionsSuccess =
    (suggestions, formName, fieldName, andIdx, orIdx, tableIdx, filterIdx) => ({
      type: actionTypes.LOAD_FILTER_SUGGESTIONS_SUCCESS,
      payload: {
        suggestions,
        formName,
        fieldName,
        andIdx,
        orIdx,
        tableIdx,
        filterIdx
      }
  });

  const loadFormFilterSuggestionsError =
    (error, formName, fieldName, andIdx, orIdx, tableIdx, filterIdx) => ({
      type: actionTypes.LOAD_FILTER_SUGGESTIONS_ERROR,
      payload: {
        ...error,
        formName,
        fieldName,
        andIdx,
        orIdx,
        tableIdx,
        filterIdx
      }
  });

  const loadFormFilterSuggestions =
    (formName, fieldName, datasetId, andIdx, orIdx, tableIdx,
      tableId, conceptId, filterIdx, filterId, prefix) => {
      return (dispatch: Dispatch) => {
        dispatch(loadFormFilterSuggestionsStart(
          formName, fieldName, andIdx, orIdx, tableIdx, conceptId, filterIdx, prefix
        ));

        return api.postPrefixForSuggestions(datasetId, conceptId, tableId, filterId, prefix)
          .then(
            r => dispatch(
              loadFormFilterSuggestionsSuccess(r, formName, fieldName, andIdx,
                orIdx, tableIdx, filterIdx)
            ),
            e => dispatch(
              loadFormFilterSuggestionsError(e, formName, fieldName, andIdx,
                orIdx, tableIdx, filterIdx)
            )
          );
      }
  }

  return {
    [`set${capitalizedType}Node`]: setNode,
    [`clear${capitalizedType}Node`]: clearNode,
    [`toggle${capitalizedType}Table`]: toggleTable,
    [`set${capitalizedType}FilterValue`]: setFilterValue,
    [`reset${capitalizedType}AllFilters`]: resetAllFilters,
    [`switch${capitalizedType}FilterMode`]: switchFilterMode,
    [`toggle${capitalizedType}Timestamps`]: toggleTimestamps,
    [`load${capitalizedType}FilterSuggestions`]: loadFilterSuggestions,
    [`load${capitalizedType}FormFilterSuggestions`]: loadFormFilterSuggestions,
  };
};
