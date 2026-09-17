import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";

import { TextAreaField, type TextAreaFieldProps } from "./TextAreaField";

export default {
  title: "FormComponents/TextAreaField",
  component: TextAreaField,
  parameters: { layout: "centered" },
} as Meta<typeof TextAreaField>;

type Story = StoryObj<typeof TextAreaField>;

const Stateful = ({
  defaultValue = "",
  ...props
}: Omit<
  Extract<TextAreaFieldProps, { label: string }>,
  "value" | "onChange"
> & {
  defaultValue?: string;
}) => {
  const [value, setValue] = useState(defaultValue);
  return <TextAreaField value={value} onChange={setValue} {...props} />;
};

export const Default: Story = {
  render: () => (
    <div className="w-80">
      <Stateful
        label="Notes"
        tooltip="Shown on the report's cover page."
        placeholder="Anything worth knowing about this report"
        rows={4}
      />
    </div>
  ),
};

export const WithError: Story = {
  render: () => (
    <div className="w-80">
      <Stateful
        label="Notes"
        defaultValue="Draft"
        rows={3}
        errorMessage="Notes need at least ten characters"
      />
    </div>
  ),
};

export const Disabled: Story = {
  render: () => (
    <div className="w-80">
      <Stateful
        label="Notes"
        defaultValue="Final version"
        rows={3}
        isDisabled
      />
    </div>
  ),
};
