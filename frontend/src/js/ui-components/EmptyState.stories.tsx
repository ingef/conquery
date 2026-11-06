import type { Meta, StoryObj } from "@storybook/react";
import { FolderOpenIcon } from "lucide-react";

import { EmptyState } from "./EmptyState";

export default {
  title: "UiComponents/EmptyState",
  component: EmptyState,
} as Meta<typeof EmptyState>;

type Story = StoryObj<typeof EmptyState>;

export const EmptyList: Story = {
  render: () => (
    <EmptyState icon={FolderOpenIcon}>No queries and forms found</EmptyState>
  ),
};
