import type { Meta, StoryObj } from "@storybook/react";
import { MegaphoneIcon } from "lucide-react";
import { Button } from "./Button";
import { Dot } from "./Dot";

export default {
  title: "UiComponents/Dot",
  component: Dot,
  parameters: { layout: "centered" },
} as Meta<typeof Dot>;

type Story = StoryObj<typeof Dot>;

export const Default: Story = {};

export const NextToText: Story = {
  render: () => (
    <p className="flex items-center gap-2 text-sm font-medium">
      <Dot />
      An unread entry
    </p>
  ),
};

export const OnAButton: Story = {
  render: () => (
    <span className="relative flex">
      <Button intent="secondary" aria-label="News, 2 unread">
        <MegaphoneIcon />
      </Button>
      <span className="absolute -top-[3px] -right-[3px] rounded-full ring-2 ring-white">
        <Dot />
      </span>
    </span>
  ),
};
