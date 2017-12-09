// @flow

import conquery                   from '../../../lib/js';
import exampleForm                from './forms/example-form';

const forms = {
  [exampleForm.type]: exampleForm
};

conquery(forms);
