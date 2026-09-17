import type { Meta, StoryObj } from "@storybook/react";
import { PlusIcon, TrashIcon } from "lucide-react";

import { Button } from "./Button";

export default {
  title: "UiComponents/Button",
  component: Button,
  parameters: { layout: "centered" },
} as Meta<typeof Button>;

type Story = StoryObj<typeof Button>;

const intents = ["primary", "secondary", "tertiary", "link"] as const;
const sizes = ["sm", "md", "lg"] as const;

export const Intents: Story = {
  render: () => (
    <div className="flex items-center gap-3">
      {intents.map((intent) => (
        <Button key={intent} intent={intent}>
          {intent}
        </Button>
      ))}
      <Button intent="primary" isDisabled>
        disabled
      </Button>
    </div>
  ),
};

export const LinkInText: Story = {
  render: () => (
    <p className="max-w-[320px] text-sm">
      A link button sits in flowing text, takes the text's size and line height,
      and wraps with it: <Button intent="link">import a list</Button> or{" "}
      <Button intent="link">
        <PlusIcon />
        add one by hand
      </Button>
      .
    </p>
  ),
};

export const Danger: Story = {
  render: () => (
    <div className="flex items-center gap-3">
      <Button intent="secondary" danger>
        <TrashIcon />
        secondary
      </Button>
      <Button intent="tertiary" danger>
        <TrashIcon />
        tertiary
      </Button>
    </div>
  ),
};

export const Sizes: Story = {
  render: () => (
    <div className="flex flex-col gap-3">
      {sizes.map((size) => (
        <div key={size} className="flex items-center gap-3">
          <Button size={size}>{size}</Button>
          <Button size={size}>
            <PlusIcon />
            with icon
          </Button>
          <Button size={size} aria-label="Delete">
            <TrashIcon />
          </Button>
          <Button size={size} intent="tertiary" aria-label="Delete">
            <TrashIcon />
          </Button>
        </div>
      ))}
    </div>
  ),
};
