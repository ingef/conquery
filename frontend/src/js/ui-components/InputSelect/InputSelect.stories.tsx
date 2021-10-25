import { ComponentMeta, Story } from "@storybook/react";
import { ComponentProps, useState } from "react";

import wordslist from "../../../fixtures/words.json";
import { SelectOptionT } from "../../api/types";

import InputSelect from "./InputSelect";

const wl = wordslist.slice(0, 100);

export default {
  title: "FormComponents/InputSelect",
  component: InputSelect,
  argTypes: {
    backgroundColor: { control: "#fafafa" },
  },
} as ComponentMeta<typeof InputSelect>;

const Template: Story<ComponentProps<typeof InputSelect>> = (args) => {
  const [options] = useState<SelectOptionT[]>(
    wl.map((w) => ({ label: w, value: w, disabled: Math.random() < 0.1 })),
  );
  const [value, setValue] = useState<SelectOptionT | null>({
    label: "Option 1",
    value: "option1",
  });

  console.log(value);

  return (
    <InputSelect
      {...args}
      value={value || null}
      onChange={(v) => setValue(v)}
      options={options}
    />
  );
};

export const Default = Template.bind({});
Default.args = {
  label: "This is a nice label",
  tooltip:
    "And here goes some tooltip that really helps the user understand what's going on",
  disabled: false,
  indexPrefix: 5,
};
Default.argTypes = {
  indexPrefix: {
    type: { name: "number", required: false },
  },
};
