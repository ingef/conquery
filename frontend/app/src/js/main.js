// @flow

import conquery                   from '../../../lib/js';
import { initializeLocalization } from '../../../lib/js/localization';
import de                         from '../../../lib/localization/de.yml';
import appDE                      from '../localization/de.yml';
import exampleForm                from './forms/example-form';


require('../styles/styles.sass');
require('../images/favicon.png');

initializeLocalization(de, appDE);

const forms = {
  [exampleForm.type]: exampleForm
};

conquery(forms);
