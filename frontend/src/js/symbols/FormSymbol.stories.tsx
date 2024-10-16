import {Meta, StoryFn} from "@storybook/react";

import FormSymbol from "./FormSymbol";

const meta = {
    title: "Symbols/FormSymbol",
    component: FormSymbol,
} as Meta<typeof FormSymbol>;

export default meta;

export const Default: StoryFn = () => <FormSymbol/>;
