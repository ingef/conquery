import { Meta, StoryFn } from "@storybook/react";

import QuerySymbol from "./QuerySymbol";

const meta = {
  title: "Symbols/QuerySymbol",
  component: QuerySymbol,
} as Meta<typeof QuerySymbol>;
export default meta;

export const Default: StoryFn = () => <QuerySymbol />;
