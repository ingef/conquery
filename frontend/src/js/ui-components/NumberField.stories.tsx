import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";

import { NumberField, type NumberFieldProps } from "./NumberField";

export default {
  title: "FormComponents/NumberField",
  component: NumberField,
  parameters: { layout: "centered" },
} as Meta<typeof NumberField>;

type Story = StoryObj<typeof NumberField>;

// each example keeps its own state and shows what the field committed
const Stateful = ({
  defaultValue = null,
  ...props
}: Omit<Extract<NumberFieldProps, { label: string }>, "value" | "onChange"> & {
  defaultValue?: number | null;
}) => {
  const [value, setValue] = useState<number | null>(defaultValue);
  return (
    <div className="flex w-64 flex-col">
      <NumberField value={value} onChange={setValue} {...props} />
      <span className="mt-10 text-xs text-gray-400">
        value: {value === null ? "null" : value}
      </span>
    </div>
  );
};

export const Default: Story = {
  render: () => (
    <Stateful
      label="Population"
      tooltip="Rounded to full thousands."
      placeholder="-"
    />
  ),
};

export const ClampedAndSnapped: Story = {
  render: () => (
    <Stateful
      label="Percentage"
      tooltip="Whole numbers from 0 to 100; anything else is corrected on blur."
      minValue={0}
      maxValue={100}
      step={1}
    />
  ),
};

// without a step any decimals pass; with one, the value snaps to it on blur
export const Step: Story = {
  render: () => (
    <div className="flex flex-col gap-4">
      <Stateful label="No step: any decimals" defaultValue={12.345} />
      <Stateful label="Step 1: whole numbers" step={1} defaultValue={12} />
      <Stateful
        label="Step 0.1"
        step={0.1}
        formatOptions={{ maximumFractionDigits: 1 }}
        defaultValue={12.3}
      />
      <Stateful
        label="Step 0.25, from a minimum of 1"
        step={0.25}
        minValue={1}
        defaultValue={2.75}
      />
    </div>
  ),
};

export const Money: Story = {
  render: () => (
    <Stateful
      label="Price"
      unit="€"
      formatOptions={{ minimumFractionDigits: 2, maximumFractionDigits: 2 }}
      defaultValue={1234.5}
    />
  ),
};

export const WithError: Story = {
  render: () => (
    <Stateful
      label="Year"
      defaultValue={1899}
      errorMessage="Data starts in 1900"
    />
  ),
};

export const Disabled: Story = {
  render: () => <Stateful label="Population" defaultValue={82000} isDisabled />,
};
