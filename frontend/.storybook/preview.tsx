import { ThemeProvider } from "@emotion/react";
import type { Decorator, Preview } from "@storybook/react";

import { theme } from "../src/app-theme";
import DndProvider from "../src/js/app/DndProvider";
import i18next from "../src/js/localization/i18next";
import translationsDe from "../src/localization/de.json";

i18next.addResourceBundle("de", "translation", translationsDe, true, true);
i18next.changeLanguage("de");

const withProviders: Decorator = (Story) => (
  <ThemeProvider theme={theme}>
    <DndProvider>
      <Story />
    </DndProvider>
  </ThemeProvider>
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
