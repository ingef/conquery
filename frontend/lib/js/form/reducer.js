// @flow

import { reducer as reduxFormReducer}        from 'redux-form';

import { createQueryRunnerReducer }     from '../query-runner';

import { SET_FORM }          from './actionTypes';
import { AVAILABLE_FORMS }              from './formTypes';

const initialState: string = AVAILABLE_FORMS.EXAMPLE_FORM;

const activeFormReducer = (state: string = initialState, action: Object): string => {
  switch (action.type) {
    case SET_FORM:
      return action.payload.form;
    default:
      return state;
  }
};

const formReducer = {
  activeForm: activeFormReducer,

  // Redux-Form reducer to keep the state of all forms:
  reduxForm: reduxFormReducer,

  // Query Runner reducer that works with form / forms
  queryRunner: createQueryRunnerReducer('form')
};

export default formReducer;
