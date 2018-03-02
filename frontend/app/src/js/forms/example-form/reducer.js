// @flow

import { combineReducers }              from 'redux';

import {
  createFormQueryNodeEditorReducer
}  from '../../../../../lib/js/external-forms/form-query-node-editor';

import {
  createFormSuggestionsReducer
} from '../../../../../lib/js/external-forms/form-suggestions/reducer';

import { type }                         from './formType';

export default combineReducers({
  example_concepts: createFormQueryNodeEditorReducer(type, 'example_concepts'),
  suggestions: createFormSuggestionsReducer(type, ['example_concepts']),
});
