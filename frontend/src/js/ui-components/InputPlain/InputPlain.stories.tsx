import { Meta, StoryObj } from "@storybook/react";
import { ComponentProps, useState } from "react";

import InputPlain from "./InputPlain";

const meta = {
  title: "FormComponents/InputPlain",
  component: InputPlain,
  argTypes: {
    backgroundColor: { control: "#fafafa" },
  },
} as Meta<typeof InputPlain>;

export default meta;

type Story = StoryObj<typeof InputPlain>;

const RenderWithString = (args: ComponentProps<typeof InputPlain>) => {
  const [value, setValue] = useState<string>("");

  console.log(value);

  return (
    <InputPlain
      {...args}
      value={value}
      onChange={(v) => setValue(v as string)}
    />
  );
};

export const WithString: Story = {
  args: {
    label: "This is a nice label",
    tooltip:
      "And here goes some tooltip that really helps the user understand what's going on",
    indexPrefix: 5,
  },
  argTypes: {
    indexPrefix: {
      type: { name: "number", required: false },
    },
  },
  render: RenderWithString,
};

const RenderWithNumber = (args: ComponentProps<typeof InputPlain>) => {
  const [value, setValue] = useState<number | null>(null);

  console.log(value);

  return (
    <InputPlain
      {...args}
      inputType="number"
      value={value || null}
      onChange={(v) => setValue(v as number | null)}
    />
  );
};

export const WithNumber: Story = {
  args: {
    label: "This is a nice label",
    tooltip:
      "And here goes some tooltip that really helps the user understand what's going on",
    indexPrefix: 5,
  },
  argTypes: {
    indexPrefix: {
      type: { name: "number", required: false },
    },
  },
  render: RenderWithNumber,
};
