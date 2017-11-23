// This module only needs to be imported to run.
// This makes it easier to handle, since other
// imported modules might already depend on a set language

import T                  from 'i18n-react';
import moment             from 'moment';

import { mergeDeep }      from '../common/helpers';

export const initializeLocalization = (...texts) => {
  T.setTexts(mergeDeep(...texts));

  // Set moment locale
  moment.locale('de');
};
