import {
  faBullseye,
  faCircle,
  faCircleDot,
  faEuroSign,
  faFolder,
  faInfo,
} from "@fortawesome/free-solid-svg-icons";
import type { Meta, StoryObj } from "@storybook/react";

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

const detailLevels = [
  { id: "summary", icon: faCircle, label: "Summary" },
  { id: "detail", icon: faCircleDot, label: "Detail" },
  { id: "full", icon: faBullseye, label: "Everything" },
];

/** one connected control, exactly one segment selected, in every size */
export const Segmented: Story = {
  render: () => (
    <div className="flex flex-col items-start gap-3">
      {sizes.map((size) => (
        <ToggleButtonGroup
          key={size}
          segmented
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

/** each segment explains itself in a tooltip */
export const SegmentedWithTooltips: Story = {
  render: () => (
    <ToggleButtonGroup
      segmented
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

/** the segments stack when the group is vertical */
export const SegmentedVertical: Story = {
  render: () => (
    <ToggleButtonGroup
      segmented
      size="sm"
      orientation="vertical"
      selectionMode="single"
      disallowEmptySelection
      defaultSelectedKeys={["week"]}
      aria-label="Period"
    >
      {periods.map(({ id, label }) => (
        <ToggleButton key={id} id={id}>
          {label}
        </ToggleButton>
      ))}
    </ToggleButtonGroup>
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
        <Icon icon={faEuroSign} />
        money
      </ToggleButton>
      <ToggleButton id="concept">
        <Icon icon={faFolder} />
        concepts
      </ToggleButton>
      <ToggleButton id="rest">
        <Icon icon={faInfo} />
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
