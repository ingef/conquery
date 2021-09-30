import { ComponentMeta, Story } from "@storybook/react";
import React, { ComponentProps, useState } from "react";

import wordslist from "../../../stories/fixtures/words.json";
import { SelectOptionT } from "../../api/types";

import InputMultiSelectTwo from "./InputMultiSelectTwo";

const wl = wordslist.slice(0, 100);

export default {
  title: "FormComponents/InputMultiSelectTwo",
  component: InputMultiSelectTwo,
  argTypes: {
    backgroundColor: { control: "#fafafa" },
  },
} as ComponentMeta<typeof InputMultiSelectTwo>;

const Template: Story<ComponentProps<typeof InputMultiSelectTwo>> = () => {
  const [options, setOptions] = useState<SelectOptionT[]>(
    wl.map((w) => ({ label: w, value: w })),
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
    <InputMultiSelectTwo
      label="This is a nice label"
      creatable
      onResolve={(csvLines) => {
        console.log(csvLines);
      }}
      tooltip="And here goes some tooltip that really helps the user understand what's going on"
      indexPrefix={5}
      input={{
        defaultValue: [],
        value: value || [],
        onChange: (v) => setValue(v),
      }}
      options={options}
    />
  );
};

export const Default = Template.bind({});
