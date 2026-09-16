import type { Meta, StoryObj } from "@storybook/react";
import { type ComponentProps, useState } from "react";

import { type ModeT, NumberRangeField } from "./NumberRangeField";

export default {
  title: "FormComponents/NumberRangeField",
  component: NumberRangeField,
  parameters: { layout: "centered" },
} as Meta<typeof NumberRangeField>;

type Story = StoryObj<typeof NumberRangeField>;

type Value = ComponentProps<typeof NumberRangeField>["value"];

// each example keeps its own value and mode, and shows what is stored
const Stateful = ({
  defaultValue = null,
  ...props
}: Omit<
  ComponentProps<typeof NumberRangeField>,
  "value" | "onChange" | "mode" | "onSwitchMode" | "disabled" | "placeholder"
> & {
  defaultValue?: Value;
  disabled?: boolean;
}) => {
  const [value, setValue] = useState<Value>(defaultValue);
  const [mode, setMode] = useState<ModeT>("range");

  return (
    <div className="flex w-72 flex-col">
      <NumberRangeField
        value={value}
        onChange={setValue}
        mode={mode}
        onSwitchMode={setMode}
        disabled={false}
        placeholder="-"
        {...props}
      />
      <span className="mt-10 text-xs text-gray-400">
        value: {JSON.stringify(value)}
      </span>
    </div>
  );
};

export const Integer: Story = {
  render: () => (
    <Stateful
      label="Number of items"
      indexPrefix={1}
      unit="#"
      tooltip="Whole numbers from 1 upwards; the shape comes from a backend pattern, the unset bound is null like the backend sends it."
      limits={{ min: 1, max: null }}
      stepSize={1}
      pattern="^(?!-)\\d*$"
    />
  ),
};

export const Real: Story = {
  render: () => (
    <Stateful
      label="Distance"
      unit="km"
      limits={{ min: 0, max: 500 }}
      stepSize={0.1}
      defaultValue={{ min: 2.5, max: 10 }}
    />
  ),
};

export const Money: Story = {
  render: () => (
    <Stateful
      label="Price"
      tooltip="Stored in cents, shown with two decimals and the unit."
      moneyRange
      currencyConfig={{
        unit: "€",
        thousandSeparator: ".",
        decimalSeparator: ",",
        decimalScale: 2,
      }}
      defaultValue={{ min: 1050, max: null }}
    />
  ),
};

export const Disabled: Story = {
  render: () => (
    <Stateful
      label="Number of items"
      unit="#"
      disabled
      defaultValue={{ min: 3, max: 12 }}
    />
  ),
};
