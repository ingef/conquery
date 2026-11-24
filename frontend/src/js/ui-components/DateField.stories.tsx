import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";

import { parseDate } from "../common/helpers/dateHelper";
import { DateField, type DateFieldProps } from "./DateField/DateField";

export default {
  title: "FormComponents/DateField",
  component: DateField,
  parameters: { layout: "centered" },
} as Meta<typeof DateField>;

type Story = StoryObj<typeof DateField>;

const DATE_FORMAT = "dd.MM.yyyy";

// the caller owns the text and decides what counts as invalid
const Stateful = ({
  defaultValue = "",
  ...props
}: Omit<
  Extract<DateFieldProps, { label: string }>,
  "value" | "onChange" | "dateFormat"
> & {
  defaultValue?: string;
}) => {
  const [value, setValue] = useState(defaultValue);
  const isInvalid = value.length > 0 && parseDate(value, DATE_FORMAT) === null;

  return (
    <DateField
      value={value}
      onChange={setValue}
      dateFormat={DATE_FORMAT}
      placeholder={DATE_FORMAT.toUpperCase()}
      errorMessage={isInvalid ? "Not a date" : undefined}
      {...props}
    />
  );
};

export const Default: Story = {
  render: () => <Stateful label="Start" tooltip="The first day that counts." />,
};

export const Invalid: Story = {
  render: () => <Stateful label="Start" defaultValue="31.02.20" />,
};

export const Disabled: Story = {
  render: () => <Stateful label="Start" defaultValue="01.01.2024" isDisabled />,
};
