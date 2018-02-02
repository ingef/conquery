// @flow

import { default as ExampleForm }          from './ExampleForm';
import reducer                             from './reducer';
import { type }                            from './formType';
import { transformExampleFormQueryToApi }  from './transformQueryToApi';

const exampleForm = {
  type,
  headline: 'externalForms.exampleForm.headline',
  order: 1,
  component: ExampleForm,
  reducer,
  transformQueryToApi: transformExampleFormQueryToApi
};

export default exampleForm;
