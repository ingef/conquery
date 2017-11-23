import _T                 from 'i18n-react';
import moment             from 'moment';

import { mergeDeep }      from '../common/helpers';

export const T = _T;

export const initializeLocalization = (...texts) => {
  T.setTexts(mergeDeep(...texts));

  // Set moment locale
  moment.locale('de');
};
