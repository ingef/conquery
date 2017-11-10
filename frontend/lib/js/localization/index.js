// This module only needs to be imported to run.
// This makes it easier to handle, since other
// imported modules might already depend on a set language

import T from 'i18n-react';
import moment from 'moment';

import de from './de.yml';
// Translation to English possible
// import en from './locales/en';


// Here could be a detection logic (e.g. check browser language, ... )
T.setTexts(de);

// Set moment locale
moment.locale('de');
