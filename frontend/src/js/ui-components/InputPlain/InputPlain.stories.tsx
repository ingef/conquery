import { ComponentMeta, Story } from "@storybook/react";
import { ComponentProps, useState } from "react";

import InputPlain from "./InputPlain";

export default {
  title: "FormComponents/InputPlain",
  component: InputPlain,
  argTypes: {
    backgroundColor: { control: "#fafafa" },
  },
} as ComponentMeta<typeof InputPlain>;

const TemplateString: Story<ComponentProps<typeof InputPlain>> = (args) => {
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

export const WithString = TemplateString.bind({});
WithString.args = {
  label: "This is a nice label",
  tooltip:
    "And here goes some tooltip that really helps the user understand what's going on",
  indexPrefix: 5,
};
WithString.argTypes = {
  indexPrefix: {
    type: { name: "number", required: false },
  },
};

const TemplateNumber: Story<ComponentProps<typeof InputPlain>> = (args) => {
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

export const WithNumber = TemplateNumber.bind({});
WithNumber.args = {
  label: "This is a nice label",
  tooltip:
    "And here goes some tooltip that really helps the user understand what's going on",
  indexPrefix: 5,
};
WithNumber.argTypes = {
  indexPrefix: {
    type: { name: "number", required: false },
  },
};
