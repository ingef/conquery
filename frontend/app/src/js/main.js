// @flow

import conquery                   from '../../../lib/js';
import { initializeLocalization } from '../../../lib/js/localization';
import de                         from '../../../lib/localization/de.yml';
import exampleForm                from './forms/example-form';

require('../styles/styles.sass');
require('../images/favicon.png');

initializeLocalization(de);

const isProduction = process.env.NODE_ENV === 'production';
const environment = {
  isProduction: isProduction,
  basename: isProduction
    ? '/' // Possibly: Run under a subpath in production
    : '/',
    apiUrl: '/api'
};

// only provides the first selected form, not an order
const forms = {
  [exampleForm.type]: exampleForm
};

conquery(environment, forms);
