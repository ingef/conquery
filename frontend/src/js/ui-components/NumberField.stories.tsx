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
    <div className="flex w-64 flex-col gap-2">
      <NumberField value={value} onChange={setValue} {...props} />
      <span className="text-xs text-gray-500">
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
      defaultValue={250}
    />
  ),
};

export const Decimals: Story = {
  render: () => (
    <Stateful
      label="Distance"
      unit="km"
      step={0.1}
      formatOptions={{ maximumFractionDigits: 1 }}
      defaultValue={12.3}
    />
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
