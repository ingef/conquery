import {
  faBan,
  faFolder,
  faThumbtack,
} from "@fortawesome/free-solid-svg-icons";
import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";

import { Icon } from "./Icon";
import { ToggleButton } from "./ToggleButton";
import { Tooltip, TooltipTrigger } from "./Tooltip";

export default {
  title: "UiComponents/ToggleButton",
  component: ToggleButton,
  parameters: { layout: "centered" },
} as Meta<typeof ToggleButton>;

type Story = StoryObj<typeof ToggleButton>;

const Demo = () => {
  const [pinned, setPinned] = useState(true);
  const [open, setOpen] = useState(false);
  const [negated, setNegated] = useState(true);

  return (
    <div className="flex items-center gap-3">
      <TooltipTrigger>
        <ToggleButton isSelected={pinned} onChange={setPinned} aria-label="Pin">
          <Icon icon={faThumbtack} />
        </ToggleButton>
        <Tooltip>Tertiary, icon only</Tooltip>
      </TooltipTrigger>
      <ToggleButton intent="secondary" isSelected={open} onChange={setOpen}>
        <Icon icon={faFolder} />
        folders
      </ToggleButton>
      <ToggleButton size="sm" danger isSelected={negated} onChange={setNegated}>
        <Icon icon={faBan} />
        negate
      </ToggleButton>
    </div>
  );
};

export const Default: Story = { render: () => <Demo /> };
