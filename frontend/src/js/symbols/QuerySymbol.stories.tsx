import { ComponentMeta, Story } from "@storybook/react";
import { ComponentProps } from "react";

import QuerySymbol from "./QuerySymbol";

export default {
  title: "Symbols/QuerySymbol",
  component: QuerySymbol,
} as ComponentMeta<typeof QuerySymbol>;

const Template: Story<ComponentProps<typeof QuerySymbol>> = () => {
  return <QuerySymbol />;
};

export const Default = Template.bind({});
Default.args = {};
