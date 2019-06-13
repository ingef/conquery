// @flow

import { default as PSMFormContainer } from './PSMFormContainer';
import reducer                         from './reducer';
import { type }                        from './formType';
import { transformPSMFormQueryToApi }  from './transformQueryToApi';

export * as actions                    from './actions';

const psmForm = {
  type,
  headline: 'externalForms.psmForm.headline',
  order: 4,
  component: PSMFormContainer,
  reducer,
  transformQueryToApi: transformPSMFormQueryToApi
};

export default psmForm;
