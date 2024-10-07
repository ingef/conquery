import { ThemeProvider } from "@emotion/react";
import type { Preview } from "@storybook/react";

import { theme } from "../src/app-theme";
import GlobalStyles from "../src/js/GlobalStyles";
import DndProvider from "../src/js/app/DndProvider";
import i18next from "../src/js/localization/i18next";
import translationsDe from "../src/localization/de.json";

i18next.addResourceBundle("de", "translation", translationsDe, true, true);
i18next.changeLanguage("de");

const Decorator = (Story: any) => (
  <ThemeProvider theme={theme}>
    <DndProvider>
      <GlobalStyles />
      <Story />
    </DndProvider>
  </ThemeProvider>
);

const preview: Preview = {
  decorators: [Decorator],
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
