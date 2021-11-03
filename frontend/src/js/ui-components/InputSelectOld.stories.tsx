import { ComponentMeta, Story } from "@storybook/react";
import { ComponentProps, useState } from "react";

import wordslist from "../../fixtures/words.json";

import InputSelectOld from "./InputSelectOld";

export default {
  title: "FormComponents/InputSelectOld",
  component: InputSelectOld,
  argTypes: {
    backgroundColor: { control: "#fafafa" },
  },
} as ComponentMeta<typeof InputSelectOld>;

interface Option {
  label: string;
  value: string;
}

const Template: Story<ComponentProps<typeof InputSelectOld>> = (args) => {
  const [options, setOptions] = useState<Option[]>(
    wordslist
      .slice(0, 100)
      .map((w) => ({ label: w, value: w, disabled: Math.random() < 0.5 })),
  );
  const [value, setValue] = useState<Option | null>({
    label: "Option 1",
    value: "option1",
  });

  return (
    <InputSelectOld
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
