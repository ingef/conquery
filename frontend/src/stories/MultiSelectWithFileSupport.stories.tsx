import { ComponentMeta, Story } from "@storybook/react";
import { SelectOptionT } from "js/api/types";
import React, { ComponentProps, useState } from "react";

import MultiSelectWithFileSupport from "../js/ui-components/MultiSelectWithFileSupport";

import wordslist from "./fixtures/words.json";

export default {
  title: "FormComponents/MultiSelectWithFileSupport",
  component: MultiSelectWithFileSupport,
  argTypes: {
    backgroundColor: { control: "#fafafa" },
  },
} as ComponentMeta<typeof MultiSelectWithFileSupport>;

const Template: Story<ComponentProps<typeof MultiSelectWithFileSupport>> = (
  args,
) => {
  const [options, setOptions] = useState<SelectOptionT[]>(
    wordslist.map((w) => ({ label: w, value: w })),
  );
  const [value, setValue] = useState<SelectOptionT[] | null>([
    {
      label: "lol",
      value: "yes",
    },
  ]);
  const onLoad = (str: string) => {
    console.log(str);
    setOptions(
      wordslist
        .filter((w) => w.startsWith(str))
        .map((w) => ({ label: w, value: w })),
    );
  };

  return (
    <MultiSelectWithFileSupport
      {...args}
      input={{
        defaultValue: [],
        value: value || [],
        onChange: (v) => setValue(v),
      }}
      onResolve={console.log}
      options={options}
      onLoad={onLoad}
    />
  );
};

export const Default = Template.bind({});
Default.args = {
  allowDropFile: true,
};
