// @flow

import { default as AUForm }          from './AUForm';
import reducer                         from './reducer';
import { type }                        from './formType';
import { transformAUFormQueryToApi }  from './transformQueryToApi';

const auForm = {
  type,
  headline: 'externalForms.auForm.headline',
  order: 1,
  component: AUForm,
  reducer,
  transformQueryToApi: transformAUFormQueryToApi
};

export default auForm;
