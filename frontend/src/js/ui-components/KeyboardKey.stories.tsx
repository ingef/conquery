import type { Meta, StoryObj } from "@storybook/react";

import { KeyboardKey } from "./KeyboardKey";

export default {
  title: "UiComponents/KeyboardKey",
  component: KeyboardKey,
  parameters: { layout: "centered" },
} as Meta<typeof KeyboardKey>;

type Story = StoryObj<typeof KeyboardKey>;

export const Single: Story = {
  render: () => <KeyboardKey>Esc</KeyboardKey>,
};

export const Combination: Story = {
  render: () => (
    <span className="flex items-center gap-1 text-sm">
      <KeyboardKey>⌘</KeyboardKey>
      <KeyboardKey>Enter</KeyboardKey>
      <span className="ml-2">runs the query</span>
    </span>
  ),
};

export const InText: Story = {
  render: () => (
    <p className="text-sm">
      Press <KeyboardKey>?</KeyboardKey> to see all shortcuts.
    </p>
  ),
};
