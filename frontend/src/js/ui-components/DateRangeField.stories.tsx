import type { Meta, StoryObj } from "@storybook/react";
import { type ComponentProps, useState } from "react";

import type { DateStringMinMax } from "../common/helpers/dateHelper";
import { DateRangeField } from "./DateRangeField";

export default {
  title: "FormComponents/DateRangeField",
  component: DateRangeField,
  parameters: { layout: "centered" },
} as Meta<typeof DateRangeField>;

type Story = StoryObj<typeof DateRangeField>;

// each example keeps its own range and shows what is stored
const Stateful = ({
  defaultValue = { min: null, max: null },
  ...props
}: Omit<ComponentProps<typeof DateRangeField>, "value" | "onChange"> & {
  defaultValue?: DateStringMinMax;
}) => {
  const [value, setValue] = useState<DateStringMinMax>(defaultValue);

  return (
    <div className="flex flex-col gap-2">
      <DateRangeField value={value} onChange={setValue} {...props} />
      <span className="text-xs text-gray-500">
        value: {JSON.stringify(value)}
      </span>
    </div>
  );
};

export const Inline: Story = {
  render: () => (
    <Stateful
      inline
      label="Period"
      tooltip="Shortcuts expand into a range, e.g. q2.2020."
    />
  ),
};

export const Stacked: Story = {
  render: () => (
    <Stateful
      label="Period"
      defaultValue={{ min: "2024-01-01", max: "2024-03-31" }}
    />
  ),
};

export const WithLabelSuffix: Story = {
  render: () => (
    <Stateful
      inline
      label="Period"
      labelSuffix={<span className="ml-2 text-xs text-gray-500">91 days</span>}
      defaultValue={{ min: "2024-01-01", max: "2024-03-31" }}
    />
  ),
};

export const WithoutLabel: Story = {
  render: () => <Stateful inline />,
};
