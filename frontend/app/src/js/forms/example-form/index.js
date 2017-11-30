// @flow

import { default as ExampleForm }          from './ExampleForm';
import reducer                             from './reducer';
import { type }                            from './formType';
import { transformExampleFormQueryToApi }  from './transformQueryToApi';

const exampleForm = {
  headline: 'form.exampleForm.headline',
  type,
  component: ExampleForm,
  reducer,
  transformQueryToApi: transformExampleFormQueryToApi
};

export default exampleForm;
