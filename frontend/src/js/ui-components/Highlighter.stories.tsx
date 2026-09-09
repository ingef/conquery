import type { Meta, StoryObj } from "@storybook/react";

import { Highlighter } from "./Highlighter";

export default {
  title: "UiComponents/Highlighter",
  component: Highlighter,
  parameters: { layout: "centered" },
} as Meta<typeof Highlighter>;

type Story = StoryObj<typeof Highlighter>;

const text = "Diabetes mellitus Typ 2 mit diabetischer Nephropathie";

export const OneWord: Story = {
  render: () => (
    <p className="text-sm">
      <Highlighter searchWords={["diab"]} textToHighlight={text} />
    </p>
  ),
};

export const SeveralWords: Story = {
  render: () => (
    <p className="text-sm">
      <Highlighter searchWords={["typ 2", "nephro"]} textToHighlight={text} />
    </p>
  ),
};

export const NoMatch: Story = {
  render: () => (
    <p className="text-sm">
      <Highlighter searchWords={["asthma"]} textToHighlight={text} />
    </p>
  ),
};
