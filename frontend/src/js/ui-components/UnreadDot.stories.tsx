import type { Meta, StoryObj } from "@storybook/react";
import { MegaphoneIcon } from "lucide-react";
import { Button } from "./Button";
import { UnreadDot } from "./UnreadDot";

export default {
  title: "UiComponents/UnreadDot",
  component: UnreadDot,
  parameters: { layout: "centered" },
} as Meta<typeof UnreadDot>;

type Story = StoryObj<typeof UnreadDot>;

export const Default: Story = {};

export const NextToText: Story = {
  render: () => (
    <p className="flex items-center gap-2 text-sm font-medium">
      <UnreadDot />
      An unread entry
    </p>
  ),
};

export const OnAButton: Story = {
  render: () => (
    <span className="group relative flex">
      <Button intent="secondary" aria-label="News, 2 unread">
        <MegaphoneIcon />
      </Button>
      <span className="absolute top-[5px] right-[5px] rounded-full ring-2 ring-white group-hover:ring-gray-50">
        <UnreadDot />
      </span>
    </span>
  ),
};
