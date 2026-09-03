import { faPlus, faTrash } from "@fortawesome/free-solid-svg-icons";
import type { Meta, StoryObj } from "@storybook/react";

import { Button } from "./Button";
import { Icon } from "./Icon";

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
        <Icon icon={faPlus} />
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
        <Icon icon={faTrash} />
        secondary
      </Button>
      <Button intent="tertiary" danger>
        <Icon icon={faTrash} />
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
            <Icon icon={faPlus} />
            with icon
          </Button>
          <Button size={size} aria-label="Delete">
            <Icon icon={faTrash} />
          </Button>
          <Button size={size} intent="tertiary" aria-label="Delete">
            <Icon icon={faTrash} />
          </Button>
        </div>
      ))}
    </div>
  ),
};
