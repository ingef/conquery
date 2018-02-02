// @flow

import { initializeLocalization } from '../../../lib/js/localization';
import translations               from '../../../lib/localization/de.yml';
import conqueryTranslations       from '../localization/de.yml';

initializeLocalization('de', translations, conqueryTranslations);

require('./main');
