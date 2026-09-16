import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";

import type { SelectOptionT } from "../api/types";
import { ComboBoxField, type ComboBoxFieldProps } from "./ComboBoxField";

export default {
  title: "FormComponents/ComboBoxField",
  component: ComboBoxField,
  parameters: { layout: "centered" },
} as Meta<typeof ComboBoxField>;

type Story = StoryObj<typeof ComboBoxField>;

const REGIONS: SelectOptionT[] = [
  { value: "north", label: "North" },
  { value: "east", label: "East" },
  { value: "south", label: "South" },
  { value: "west", label: "West" },
  { value: "central", label: "Central", disabled: true },
];

// each example keeps its own selection and shows what was picked
const Stateful = ({
  defaultValue = null,
  ...props
}: Omit<
  Extract<ComboBoxFieldProps, { label: string }>,
  "value" | "onChange"
> & {
  defaultValue?: SelectOptionT | null;
}) => {
  const [value, setValue] = useState<SelectOptionT | null>(defaultValue);
  return (
    <div className="flex w-72 flex-col gap-2">
      <ComboBoxField value={value} onChange={setValue} {...props} />
      <span className="text-xs text-gray-500">
        value: {value ? String(value.value) : "null"}
      </span>
    </div>
  );
};

export const Default: Story = {
  render: () => (
    <Stateful
      label="Region"
      tooltip="Type to filter; Central cannot be picked."
      options={REGIONS}
    />
  ),
};

export const Preselected: Story = {
  render: () => (
    <Stateful label="Region" options={REGIONS} defaultValue={REGIONS[2]} />
  ),
};

export const MarkdownLabels: Story = {
  render: () => (
    <Stateful
      label="Product"
      options={[
        { value: "a", label: "**Alpha** – the basic tier" },
        { value: "b", label: "**Beta** – the extended tier" },
        { value: "c", label: "**Gamma** – everything" },
      ]}
    />
  ),
};

export const SelectedOptionMissingFromTheList: Story = {
  render: () => (
    <Stateful
      label="Region"
      tooltip="The selected option is offered even though the list does not hold it."
      options={REGIONS.slice(0, 2)}
      defaultValue={{ value: "island", label: "Island" }}
    />
  ),
};

export const NoOptions: Story = {
  render: () => <Stateful label="Region" options={[]} />,
};

export const WithError: Story = {
  render: () => (
    <Stateful label="Region" options={REGIONS} errorMessage="Pick a region" />
  ),
};

export const Disabled: Story = {
  render: () => (
    <Stateful
      label="Region"
      options={REGIONS}
      defaultValue={REGIONS[0]}
      isDisabled
    />
  ),
};
