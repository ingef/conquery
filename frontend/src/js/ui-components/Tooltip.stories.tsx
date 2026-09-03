import { faInfoCircle, faTrash } from "@fortawesome/free-solid-svg-icons";
import type { Meta, StoryObj } from "@storybook/react";

import IconButton from "../button/IconButton";
import PrimaryButton from "../button/PrimaryButton";
import { TransparentButton } from "../button/TransparentButton";
import FaIcon from "../icon/FaIcon";

import { Tooltip, TooltipTarget, TooltipTrigger } from "./Tooltip";

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
        <IconButton frame icon={faTrash} />
        <Tooltip>Delete</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger>
        <TransparentButton>Cancel</TransparentButton>
        <Tooltip>Discards your changes</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger>
        <PrimaryButton>Save</PrimaryButton>
        <Tooltip>Saves and closes the editor</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger>
        <PrimaryButton disabled>Disabled</PrimaryButton>
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
          <TransparentButton small>{placement}</TransparentButton>
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
        <TransparentButton>Warm-up</TransparentButton>
        <Tooltip>Waits for the warm-up, then neighbours open instantly</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger>
        <IconButton frame icon={faTrash} />
        <Tooltip>Icon-only buttons wait too</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger delay={0}>
        <TooltipTarget role="img" aria-label="Info">
          <FaIcon gray icon={faInfoCircle} />
        </TooltipTarget>
        <Tooltip>Help icons open immediately</Tooltip>
      </TooltipTrigger>
    </div>
  ),
};

export const RichContent: Story = {
  render: () => (
    <TooltipTrigger>
      <TransparentButton>Rich content</TransparentButton>
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
      <TooltipTrigger delay={0}>
        <TooltipTarget role="img" aria-label="Info">
          <FaIcon gray icon={faInfoCircle} />
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
