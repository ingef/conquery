// @flow

import { default as DescriptionForm }          from './DescriptionForm';
import reducer                                 from './reducer';
import { type }                                from './formType';
import { transformDescriptionFormQueryToApi }  from './transformQueryToApi';

export * as actions                            from './actions';

const descriptionForm = {
  type,
  headline: 'externalForms.descriptionForm.headline',
  order: 2,
  component: DescriptionForm,
  reducer,
  transformQueryToApi: transformDescriptionFormQueryToApi
};

export default descriptionForm;
