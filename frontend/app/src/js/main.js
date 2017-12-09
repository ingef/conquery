// @flow

import conquery                   from '../../../lib/js';
import exampleForm                from './forms/example-form';


require('../styles/styles.sass');
require('../images/favicon.png');

const forms = {
  [exampleForm.type]: exampleForm
};

conquery(forms);
