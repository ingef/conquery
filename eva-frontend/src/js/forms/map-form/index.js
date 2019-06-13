// @flow

import { default as MapForm }          from './MapForm';
import reducer                         from './reducer';
import { type }                        from './formType';
import { transformMapFormQueryToApi }  from './transformQueryToApi';

const mapForm = {
  type,
  headline: 'externalForms.mapForm.headline',
  order: 5,
  component: MapForm,
  reducer,
  transformQueryToApi: transformMapFormQueryToApi
};

export default mapForm;
