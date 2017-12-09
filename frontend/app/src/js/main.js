// @flow

import conquery                   from '../../../lib/js';
import exampleForm                from './forms/example-form';


const isProduction = process.env.NODE_ENV === 'production';
const environment = {
  isProduction: isProduction,
  basename: isProduction
    ? '/' // Possibly: Run under a subpath in production
    : '/',
    apiUrl: '/api'
};

const forms = {
  [exampleForm.type]: exampleForm
};

conquery(environment, forms);
