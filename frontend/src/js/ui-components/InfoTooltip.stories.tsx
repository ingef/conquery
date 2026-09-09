import type { Meta, StoryObj } from "@storybook/react";

import InfoTooltip from "./InfoTooltip";

export default {
  title: "UiComponents/InfoTooltip",
  component: InfoTooltip,
  parameters: { layout: "centered" },
} as Meta<typeof InfoTooltip>;

type Story = StoryObj<typeof InfoTooltip>;

export const AfterALabel: Story = {
  render: () => (
    <span className="flex items-center text-sm">
      Analysis layer
      <InfoTooltip text="Joins the concepts on the chosen id instead of the person." />
    </span>
  ),
};

export const WithMarkup: Story = {
  render: () => (
    <span className="flex items-center text-sm">
      Upload format
      <InfoTooltip
        wide
        text="<p>One id per line.</p><ul><li><code>2020-01-01</code></li><li><code>01.01.2020</code></li></ul>"
      />
    </span>
  ),
};

export const WithElement: Story = {
  render: () => (
    <span className="flex items-center text-sm">
      Status
      <InfoTooltip
        html={
          <span>
            Set in the <strong>settings</strong> of the history.
          </span>
        }
      />
    </span>
  ),
};
