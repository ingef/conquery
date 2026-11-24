import type { Meta, StoryObj } from "@storybook/react";

import { Highlighter } from "./Highlighter";

export default {
  title: "UiComponents/Highlighter",
  component: Highlighter,
  parameters: { layout: "centered" },
} as Meta<typeof Highlighter>;

type Story = StoryObj<typeof Highlighter>;

const text = "Population density by region and year";

export const OneWord: Story = {
  render: () => (
    <p className="text-sm">
      <Highlighter searchWords={["pop"]} textToHighlight={text} />
    </p>
  ),
};

export const SeveralWords: Story = {
  render: () => (
    <p className="text-sm">
      <Highlighter searchWords={["density", "year"]} textToHighlight={text} />
    </p>
  ),
};

export const NoMatch: Story = {
  render: () => (
    <p className="text-sm">
      <Highlighter searchWords={["income"]} textToHighlight={text} />
    </p>
  ),
};
