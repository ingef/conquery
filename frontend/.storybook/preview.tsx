import type { Decorator, Preview } from "@storybook/react";

import "../src/index.css";

import { theme } from "../src/app-theme";
import DndProvider from "../src/js/app/DndProvider";
import { AppThemeContext } from "../src/js/app-theme-context";
import i18next from "../src/js/localization/i18next";
import translationsDe from "../src/localization/de.json";

i18next.addResourceBundle("de", "translation", translationsDe, true, true);
i18next.changeLanguage("de");

const withProviders: Decorator = (Story) => (
  <AppThemeContext.Provider value={theme}>
    <DndProvider>
      <Story />
    </DndProvider>
  </AppThemeContext.Provider>
);

const preview: Preview = {
  decorators: [withProviders],
  parameters: {
    actions: { argTypesRegex: "^on[A-Z].*" },
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
};

export default preview;
