import { ComponentMeta, Story } from "@storybook/react";
import { ComponentProps } from "react";

import FormSymbol from "./FormSymbol";

export default {
  title: "Symbols/FormSymbol",
  component: FormSymbol,
} as ComponentMeta<typeof FormSymbol>;

const Template: Story<ComponentProps<typeof FormSymbol>> = () => {
  return <FormSymbol />;
};

export const Default = Template.bind({});
Default.args = {};
