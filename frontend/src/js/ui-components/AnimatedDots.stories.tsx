import type { Meta, StoryObj } from "@storybook/react";

import AnimatedDots from "./AnimatedDots";

export default {
  title: "UiComponents/AnimatedDots",
  component: AnimatedDots,
  parameters: { layout: "centered" },
} as Meta<typeof AnimatedDots>;

type Story = StoryObj<typeof AnimatedDots>;

export const Default: Story = {
  render: () => <AnimatedDots />,
};

export const AfterText: Story = {
  render: () => (
    <span className="text-sm">
      Loading
      <AnimatedDots />
    </span>
  ),
};
