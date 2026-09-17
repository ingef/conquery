import { faUndo } from "@fortawesome/free-solid-svg-icons";
import type { Meta, StoryObj } from "@storybook/react";
import { type ComponentProps, useState } from "react";

import type { DateStringMinMax } from "../common/helpers/dateHelper";
import { Button } from "./Button";
import { DateRangeField } from "./DateRangeField";
import { Icon } from "./Icon";

export default {
  title: "FormComponents/DateRangeField",
  component: DateRangeField,
  parameters: { layout: "centered" },
} as Meta<typeof DateRangeField>;

type Story = StoryObj<typeof DateRangeField>;

const empty: DateStringMinMax = { min: null, max: null };

// each example keeps its own range and shows what is stored
const Stateful = ({
  defaultValue = empty,
  withReset,
  ...props
}: Omit<
  ComponentProps<typeof DateRangeField>,
  "value" | "onChange" | "labelSuffix"
> & {
  defaultValue?: DateStringMinMax;
  withReset?: boolean;
}) => {
  const [value, setValue] = useState<DateStringMinMax>(defaultValue);
  const hasDate = !!(value.min || value.max);

  return (
    <div className="flex flex-col">
      <DateRangeField
        value={value}
        onChange={setValue}
        labelSuffix={
          withReset &&
          hasDate && (
            <span className="ml-5">
              <Button intent="link" onPress={() => setValue(empty)}>
                <Icon icon={faUndo} />
                Reset
              </Button>
            </span>
          )
        }
        {...props}
      />
      <span className="mt-10 text-xs text-gray-400">
        value: {JSON.stringify(value)}
      </span>
    </div>
  );
};

export const Default: Story = {
  render: () => (
    <Stateful
      label="Period"
      tooltip="Shortcuts expand into a range, e.g. q2.2020."
    />
  ),
};

// the app puts a reset link after the label while a date is set
export const WithLabelSuffix: Story = {
  render: () => (
    <Stateful
      label="Period"
      withReset
      defaultValue={{ min: "2024-01-01", max: "2024-03-31" }}
    />
  ),
};

export const WithoutLabel: Story = {
  render: () => <Stateful />,
};

export const Disabled: Story = {
  render: () => (
    <Stateful
      label="Period"
      isDisabled
      defaultValue={{ min: "2024-01-01", max: "2024-03-31" }}
    />
  ),
};
