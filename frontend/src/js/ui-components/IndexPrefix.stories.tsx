import type { Meta, StoryObj } from "@storybook/react";

import { IndexPrefix } from "./IndexPrefix";

export default {
  title: "UiComponents/IndexPrefix",
  component: IndexPrefix,
  parameters: { layout: "centered" },
} as Meta<typeof IndexPrefix>;

type Story = StoryObj<typeof IndexPrefix>;

export const Default: Story = {
  render: () => <IndexPrefix>#1</IndexPrefix>,
};

export const BeforeLabels: Story = {
  render: () => (
    <div className="flex flex-col gap-2 text-sm">
      {["Age", "Gender", "Diagnosis"].map((label, i) => (
        <div key={label} className="flex items-center">
          <IndexPrefix>#{i + 1}</IndexPrefix>
          {label}
        </div>
      ))}
    </div>
  ),
};
