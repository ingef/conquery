import type { Meta, StoryObj } from "@storybook/react";

import InfoTooltip from "./InfoTooltip";
import Label from "./Label";

export default {
  title: "UiComponents/InfoTooltip",
  component: InfoTooltip,
  parameters: { layout: "centered" },
} as Meta<typeof InfoTooltip>;

type Story = StoryObj<typeof InfoTooltip>;

export const AfterALabel: Story = {
  render: () => (
    <Label>
      Analysis layer
      <InfoTooltip text="Joins the concepts on the chosen id instead of the person." />
    </Label>
  ),
};

export const WithMarkup: Story = {
  render: () => (
    <Label>
      Upload format
      <InfoTooltip
        size="wide"
        text="<p>One id per line.</p><ul><li><code>2020-01-01</code></li><li><code>01.01.2020</code></li></ul>"
      />
    </Label>
  ),
};

export const WithElement: Story = {
  render: () => (
    <Label>
      Status
      <InfoTooltip
        html={
          <span>
            Set in the <strong>settings</strong> of the history.
          </span>
        }
      />
    </Label>
  ),
};
