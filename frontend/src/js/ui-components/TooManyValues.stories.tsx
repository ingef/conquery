import type { Meta, StoryObj } from "@storybook/react";

import TooManyValues from "./TooManyValues";

export default {
  title: "UiComponents/TooManyValues",
  component: TooManyValues,
  parameters: { layout: "centered" },
} as Meta<typeof TooManyValues>;

type Story = StoryObj<typeof TooManyValues>;

export const Default: Story = {
  render: () => <TooManyValues count={1234} onClear={() => {}} />,
};
