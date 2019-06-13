// @flow

import { combineReducers }              from 'redux';
import {
  createFormQueryNodeEditorReducer
}                                       from '../../external-forms/form-query-node-editor';
import { createFormSuggestionsReducer } from '../../external-forms/form-suggestions/reducer';

import { type }                         from './formType';

export default combineReducers({
  features: createFormQueryNodeEditorReducer(type, 'features'),
  suggestions: createFormSuggestionsReducer(type, ['features']),
});
