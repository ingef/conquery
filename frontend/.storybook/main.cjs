const path = require("path");
const { mergeConfig } = require("vite");
const toPath = (_path) => path.resolve(path.join(__dirname, _path));

module.exports = {
  stories: ["../src/**/*.mdx", "../src/**/*.stories.@(js|jsx|ts|tsx)"],
  addons: [
    "@storybook/addon-links",
    "@storybook/addon-essentials",
    "@storybook/addon-interactions",
  ],
  framework: {
    name: "@storybook/react-vite",
    options: {},
  },
  features: {
    storyStoreV7: true,
  },
  core: {
    disableTelemetry: true,
  },
  docs: {
    autodocs: true,
  },
};
