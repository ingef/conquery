// @flow

import { combineReducers }              from 'redux';
import { reducer as reduxFormReducer }  from 'redux-form';

import { createQueryRunnerReducer }     from '../query-runner';

import { SET_EXTERNAL_FORM }            from './actionTypes';
import { AVAILABLE_FORMS }              from './externalFormTypes';

// SPECIFIC FORM REDUCERS
// import { reducer as exampleReducer } from './example-form';

const initialState: string = AVAILABLE_FORMS.EXAMPLE_FORM;

const activeFormReducer = (state: string = initialState, action: Object): string => {
  switch (action.type) {
    case SET_EXTERNAL_FORM:
      return action.payload.form;
    default:
      return state;
  }
};

const externalFormsReducer = combineReducers({
  activeForm: activeFormReducer,

  // Redux-Form reducer to keep the state of all forms:
  reduxForm: reduxFormReducer,

  // Query Runner reducer that works with forms
  queryRunner: createQueryRunnerReducer('externalForms')

  // More specific reducers for anything that shouldn't be part of the form
  // Like a modal open state in one of the forms
  //
  // example: exampleReducer
});

export default externalFormsReducer;
