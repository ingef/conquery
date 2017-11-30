// @flow

import { reducer as reduxFormReducer }  from 'redux-form';

import { createQueryRunnerReducer }     from '../query-runner';

import { SET_FORM }                     from './actionTypes';

const activeFormReducer = (state: string = '', action: Object): string => {
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
  queryRunner: createQueryRunnerReducer('form'),

  // available forms store added at conquery initialization
  // in the future this can allow adding forms in runtime
  availableForms: (state = {}) => state
};

export default formReducer;
