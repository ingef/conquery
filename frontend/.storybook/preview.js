import { ThemeProvider } from '@emotion/react';
import { theme } from '../src/app-theme';
import React from 'react';
import { addDecorator } from '@storybook/react';

import DndProvider from '../src/js/app/DndProvider';

import i18next from "../src/js/localization/i18next";
import translationsDe from "../src/localization/de.json";
i18next.addResourceBundle("de", "translation", translationsDe, true, true);
i18next.changeLanguage("de");

export const parameters = {
  actions: { argTypesRegex: "^on[A-Z].*" },
  controls: {
    matchers: {
      color: /(background|color)$/i,
      date: /Date$/,
    },
  },
}

addDecorator((Story) => {
  return (
    <ThemeProvider theme={theme}>
    <DndProvider>
      <Story /></DndProvider>
    </ThemeProvider>
  );
})