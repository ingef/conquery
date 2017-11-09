// @flow

import { combineReducers }              from 'redux';
import { reducer as formReducer}        from 'redux-form';

import { createQueryRunnerReducer }     from '../query-runner';

import { SET_STATISTICS_FORM }          from './actionTypes';
import { AVAILABLE_FORMS }              from './statisticsFormTypes';

// SPECIFIC FORM REDUCERS
// import { reducer as exampleReducer } from './example-form';

const initialState: string = AVAILABLE_FORMS.EXAMPLE_FORM;

const activeFormReducer = (state: string = initialState, action: Object): string => {
  switch (action.type) {
    case SET_STATISTICS_FORM:
      return action.payload.form;
    default:
      return state;
  }
};

const statisticsReducer = combineReducers({
  activeForm: activeFormReducer,

  // Redux-Form reducer to keep the state of all forms:
  form: formReducer,

  // Query Runner reducer that works with statistics / forms
  queryRunner: createQueryRunnerReducer('statistics')

  // More specific reducers for anything that shouldn't be part of the form
  // Like a modal open state in one of the forms
  //
  // example: exampleReducer
});

export default statisticsReducer;
