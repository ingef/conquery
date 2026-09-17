import type { Meta, StoryObj } from "@storybook/react";
import {
  CircleDotIcon,
  CircleIcon,
  EuroIcon,
  FolderIcon,
  InfoIcon,
  TargetIcon,
} from "lucide-react";

import { Icon } from "./Icon";
import { ToggleButton } from "./ToggleButton";
import { ToggleButtonGroup } from "./ToggleButtonGroup";
import { Tooltip, TooltipTrigger } from "./Tooltip";

export default {
  title: "UiComponents/ToggleButtonGroup",
  component: ToggleButtonGroup,
  parameters: { layout: "centered" },
} as Meta<typeof ToggleButtonGroup>;

type Story = StoryObj<typeof ToggleButtonGroup>;

const sizes = ["sm", "md", "lg"] as const;

const periods = [
  { id: "day", label: "Day", description: "One row per day" },
  { id: "week", label: "Week", description: "One row per week" },
  { id: "month", label: "Month", description: "One row per month" },
];

const regions = [
  "North",
  "South",
  "East",
  "West",
  "Central",
  "Coast",
  "Highlands",
  "Islands",
];

const detailLevels = [
  { id: "summary", icon: CircleIcon, label: "Summary" },
  { id: "detail", icon: CircleDotIcon, label: "Detail" },
  { id: "full", icon: TargetIcon, label: "Everything" },
];

/** exactly one button selected, in every size */
export const SingleSelection: Story = {
  render: () => (
    <div className="flex flex-col items-start gap-3">
      {sizes.map((size) => (
        <ToggleButtonGroup
          key={size}
          size={size}
          selectionMode="single"
          disallowEmptySelection
          defaultSelectedKeys={["week"]}
          aria-label={`Period, ${size}`}
        >
          {periods.map(({ id, label }) => (
            <ToggleButton key={id} id={id}>
              {label}
            </ToggleButton>
          ))}
        </ToggleButtonGroup>
      ))}
    </div>
  ),
};

/** each option explains itself in a tooltip */
export const WithTooltips: Story = {
  render: () => (
    <ToggleButtonGroup
      size="sm"
      selectionMode="single"
      disallowEmptySelection
      defaultSelectedKeys={["day"]}
      aria-label="Period"
    >
      {periods.map(({ id, label, description }) => (
        <TooltipTrigger key={id}>
          <ToggleButton id={id}>{label}</ToggleButton>
          <Tooltip>{description}</Tooltip>
        </TooltipTrigger>
      ))}
    </ToggleButtonGroup>
  ),
};

/** options that come from data continue on the next line when the group may wrap */
export const Wrapping: Story = {
  render: () => (
    <div className="flex w-72 flex-col gap-3 border border-dashed border-gray-400 p-3">
      <ToggleButtonGroup
        wrap
        size="sm"
        selectionMode="single"
        disallowEmptySelection
        defaultSelectedKeys={["North"]}
        aria-label="Region"
      >
        {regions.map((region) => (
          <ToggleButton key={region} id={region}>
            {region}
          </ToggleButton>
        ))}
      </ToggleButtonGroup>
    </div>
  ),
};

/** separate buttons, any number of them on: a filter */
export const MultipleSelection: Story = {
  render: () => (
    <ToggleButtonGroup
      selectionMode="multiple"
      defaultSelectedKeys={["money", "concept"]}
      aria-label="Content"
    >
      <ToggleButton id="money">
        <Icon icon={EuroIcon} />
        money
      </ToggleButton>
      <ToggleButton id="concept">
        <Icon icon={FolderIcon} />
        concepts
      </ToggleButton>
      <ToggleButton id="rest">
        <Icon icon={InfoIcon} />
        other
      </ToggleButton>
    </ToggleButtonGroup>
  ),
};

/** a vertical toolbar of icon toggles, one of them on, tooltips to the side */
export const VerticalIcons: Story = {
  render: () => (
    <ToggleButtonGroup
      orientation="vertical"
      selectionMode="single"
      disallowEmptySelection
      defaultSelectedKeys={["summary"]}
      aria-label="Detail level"
    >
      {detailLevels.map(({ id, icon, label }) => (
        <TooltipTrigger key={id}>
          <ToggleButton id={id} aria-label={label}>
            <Icon icon={icon} />
          </ToggleButton>
          <Tooltip placement="right">{label}</Tooltip>
        </TooltipTrigger>
      ))}
    </ToggleButtonGroup>
  ),
};
