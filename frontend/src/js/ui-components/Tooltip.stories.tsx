import { faInfoCircle, faTrash } from "@fortawesome/free-solid-svg-icons";
import type { Meta, StoryObj } from "@storybook/react";
import { Button } from "./Button";
import { Icon } from "./Icon";

import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
  tooltipDelay,
} from "./Tooltip";

export default {
  title: "UiComponents/Tooltip",
  component: Tooltip,
  parameters: { layout: "centered" },
} as Meta<typeof Tooltip>;

type Story = StoryObj<typeof Tooltip>;

export const OnButtons: Story = {
  render: () => (
    <div className="flex items-center gap-4">
      <TooltipTrigger>
        <Button intent="secondary">
          <Icon icon={faTrash} />
        </Button>
        <Tooltip>Delete</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger>
        <Button intent="secondary">Cancel</Button>
        <Tooltip>Discards your changes</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger>
        <Button intent="primary">Save</Button>
        <Tooltip>Saves and closes the editor</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger>
        <Button intent="primary" isDisabled>
          Disabled
        </Button>
        <Tooltip>Disabled buttons show no tooltip</Tooltip>
      </TooltipTrigger>
    </div>
  ),
};

export const Placements: Story = {
  render: () => (
    <div className="flex items-center gap-4">
      {(["top", "bottom", "left", "right"] as const).map((placement) => (
        <TooltipTrigger key={placement}>
          <Button intent="secondary" size="sm">
            {placement}
          </Button>
          <Tooltip placement={placement}>Placed at {placement}</Tooltip>
        </TooltipTrigger>
      ))}
    </div>
  ),
};

export const Timing: Story = {
  render: () => (
    <div className="flex items-center gap-4 text-sm">
      <TooltipTrigger>
        <Button intent="secondary">
          <Icon icon={faTrash} />
        </Button>
        <Tooltip>
          Names a control: short warm-up, neighbors open instantly
        </Tooltip>
      </TooltipTrigger>
      <TooltipTrigger delay={tooltipDelay.long}>
        <Button intent="secondary">Further info</Button>
        <Tooltip>Explains a larger surface: long warm-up</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger delay={tooltipDelay.immediate}>
        <TooltipTarget role="img" aria-label="Info">
          <Icon icon={faInfoCircle} className="text-gray-500" />
        </TooltipTarget>
        <Tooltip>Help icons open immediately</Tooltip>
      </TooltipTrigger>
    </div>
  ),
};

export const RichContent: Story = {
  render: () => (
    <TooltipTrigger>
      <Button intent="secondary">Rich content</Button>
      <Tooltip wide>
        <h3>Headline</h3>
        <p>
          A wide tooltip can hold formatted content: paragraphs, headlines and
          lists.
        </p>
        <ul>
          <li>First point</li>
          <li>Second point</li>
        </ul>
      </Tooltip>
    </TooltipTrigger>
  ),
};

export const OnStaticContent: Story = {
  render: () => (
    <div className="flex items-center gap-4 text-sm">
      <TooltipTrigger delay={tooltipDelay.immediate}>
        <TooltipTarget role="img" aria-label="Info">
          <Icon icon={faInfoCircle} className="text-gray-500" />
        </TooltipTarget>
        <Tooltip>An icon that is reachable with the keyboard</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger>
        <TooltipTarget
          excludeFromTabOrder
          className="underline decoration-dotted"
        >
          Static text
        </TooltipTarget>
        <Tooltip>Hover only, stays out of the tab order</Tooltip>
      </TooltipTrigger>
    </div>
  ),
};
