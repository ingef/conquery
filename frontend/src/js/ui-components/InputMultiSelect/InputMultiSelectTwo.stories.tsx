import { ComponentMeta, Story } from "@storybook/react";
import React, { ComponentProps, useState } from "react";

import wordslist from "../../../fixtures/words.json";
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

const Template: Story<
  ComponentProps<typeof InputMultiSelectTwo> & { passOnResolve?: boolean }
> = ({ passOnResolve, ...args }) => {
  const [options, setOptions] = useState<SelectOptionT[]>(
    wl.map((w) => ({ label: w, value: w, disabled: Math.random() < 0.1 })),
  );
  const [value, setValue] = useState<SelectOptionT[] | null>([
    {
      label: "lol",
      value: "yes",
    },
  ]);
  const onLoad = (str: string) => {
    setOptions(
      wordslist
        .filter((w) => w.startsWith(str))
        .map((w) => ({ label: w, value: w })),
    );
  };

  const onResolve = (csvLines: any) => {
    console.log(csvLines);
  };

  return (
    <InputMultiSelectTwo
      {...args}
      onResolve={passOnResolve ? onResolve : undefined}
      onLoadMore={() => {
        console.log("ON LOAD MORE");
      }}
      creatable
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
Default.args = {
  indexPrefix: 5,
  label: "This is a nice label",
  tooltip:
    "And here goes some tooltip that really helps the user understand what's going on",
  disabled: false,
  passOnResolve: true,
};
Default.argTypes = {
  passOnResolve: {
    type: { name: "boolean" },
  },
  indexPrefix: {
    type: { name: "number", required: false },
  },
};
