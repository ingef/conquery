import type { Meta, StoryObj } from "@storybook/react";

import ProgressBar from "./ProgressBar";

export default {
  title: "UiComponents/ProgressBar",
  component: ProgressBar,
  parameters: { layout: "centered" },
} as Meta<typeof ProgressBar>;

type Story = StoryObj<typeof ProgressBar>;

export const Steps: Story = {
  render: () => (
    <div className="flex w-64 flex-col gap-3">
      {[0, 25, 50, 75, 100].map((donePercent) => (
        <div key={donePercent} className="flex items-center gap-3">
          <span className="w-10 text-right text-xs text-gray-500">
            {donePercent}%
          </span>
          <ProgressBar donePercent={donePercent} />
        </div>
      ))}
    </div>
  ),
};
