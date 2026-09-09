import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";

import { Checkbox, type CheckboxProps } from "./Checkbox";

export default {
  title: "UiComponents/Checkbox",
  component: Checkbox,
  parameters: { layout: "centered" },
} as Meta<typeof Checkbox>;

type Story = StoryObj<typeof Checkbox>;

// each example keeps its own state so it can be switched in the story
const Stateful = ({
  defaultSelected = false,
  ...props
}: Omit<CheckboxProps, "isSelected" | "onChange"> & {
  defaultSelected?: boolean;
}) => {
  const [selected, setSelected] = useState(defaultSelected);
  return <Checkbox isSelected={selected} onChange={setSelected} {...props} />;
};

export const Default: Story = {
  render: () => (
    <div className="flex flex-col gap-1">
      <Stateful>Include unresolved codes</Stateful>
      <Stateful defaultSelected>Include unresolved codes</Stateful>
    </div>
  ),
};

export const Disabled: Story = {
  render: () => (
    <div className="flex flex-col gap-1">
      <Stateful
        isDisabled
        tooltip="All codes are unresolved, so they are included."
      >
        Include unresolved codes
      </Stateful>
      <Stateful
        isDisabled
        defaultSelected
        tooltip="All codes are unresolved, so they are included."
      >
        Include unresolved codes
      </Stateful>
    </div>
  ),
};

export const WithInfoTooltip: Story = {
  render: () => (
    <Stateful infoTooltip="Rows without a date are left out of the time calculation.">
      Exclude from time calculation
    </Stateful>
  ),
};

export const MultiLine: Story = {
  render: () => (
    <div className="w-64">
      <Stateful defaultSelected>
        A long label that wraps onto a second and a third line keeps the box on
        its first line
      </Stateful>
    </div>
  ),
};
