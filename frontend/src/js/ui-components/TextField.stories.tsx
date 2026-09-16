import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";

import { TextField, type TextFieldProps } from "./TextField";

export default {
  title: "FormComponents/TextField",
  component: TextField,
  parameters: { layout: "centered" },
} as Meta<typeof TextField>;

type Story = StoryObj<typeof TextField>;

// each example keeps its own state so it can be typed into
const Stateful = ({
  defaultValue = "",
  ...props
}: Omit<Extract<TextFieldProps, { label: string }>, "value" | "onChange"> & {
  defaultValue?: string;
}) => {
  const [value, setValue] = useState(defaultValue);
  return <TextField value={value} onChange={setValue} {...props} />;
};

export const Default: Story = {
  render: () => (
    <div className="w-72">
      <Stateful
        label="Region"
        tooltip="The region the report covers."
        placeholder="North, South, …"
      />
    </div>
  ),
};

export const WithError: Story = {
  render: () => (
    <div className="w-72">
      <Stateful
        label="Region"
        defaultValue="Nrth"
        errorMessage="Unknown region"
      />
    </div>
  ),
};

export const Disabled: Story = {
  render: () => (
    <div className="w-72">
      <Stateful label="Region" defaultValue="North" isDisabled />
    </div>
  ),
};

export const Password: Story = {
  render: () => (
    <div className="w-72">
      <Stateful label="Password" type="password" defaultValue="secret" />
    </div>
  ),
};

export const InANumberedList: Story = {
  render: () => (
    <div className="flex w-72 flex-col">
      <Stateful label="Region" indexPrefix={1} />
      <Stateful label="Year" indexPrefix={2} />
    </div>
  ),
};
