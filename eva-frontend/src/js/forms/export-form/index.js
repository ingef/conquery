// @flow

import { default as ExportForm }          from './ExportForm';
import reducer                            from './reducer';
import { type }                           from './formType';
import { transformExportFormQueryToApi }  from './transformQueryToApi';

export * as actions                       from './actions';

const exportForm = {
  type,
  headline: 'externalForms.exportForm.headline',
  order: 3,
  component: ExportForm,
  reducer,
  transformQueryToApi: transformExportFormQueryToApi
};

export default exportForm;
