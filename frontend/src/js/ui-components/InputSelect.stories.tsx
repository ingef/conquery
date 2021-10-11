import { ComponentMeta, Story } from "@storybook/react";
import React, { ComponentProps, useState } from "react";

import wordslist from "../../fixtures/words.json";

import InputSelect from "./InputSelect";

export default {
  title: "FormComponents/InputSelect",
  component: InputSelect,
  argTypes: {
    backgroundColor: { control: "#fafafa" },
  },
} as ComponentMeta<typeof InputSelect>;

interface Option {
  label: string;
  value: string;
}

const Template: Story<ComponentProps<typeof InputSelect>> = (args) => {
  const [options, setOptions] = useState<Option[]>(
    wordslist
      .slice(0, 100)
      .map((w) => ({ label: w, value: w, disabled: Math.random() < 0.5 })),
  );
  const [value, setValue] = useState<Option | null>({
    label: "lol",
    value: "yes",
  });

  return (
    <InputSelect
      {...args}
      input={{
        value: value?.value || null,
        onChange: (v) => setValue(options.find((o) => o.value === v) || null),
      }}
      options={options}
    />
  );
};

export const Default = Template.bind({});
