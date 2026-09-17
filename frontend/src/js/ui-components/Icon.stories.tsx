import type { Meta, StoryObj } from "@storybook/react";
import {
  CheckIcon,
  EllipsisVerticalIcon,
  FolderIcon,
  FolderOpenIcon,
  LoaderCircleIcon,
  SquareIcon,
  TrashIcon,
  UnfoldHorizontalIcon,
  UserIcon,
} from "lucide-react";
import { Button } from "./Button";

import { Icon } from "./Icon";

export default {
  title: "UiComponents/Icon",
  component: Icon,
  parameters: { layout: "centered" },
} as Meta<typeof Icon>;

type Story = StoryObj<typeof Icon>;

export const InText: Story = {
  render: () => (
    <ul className="flex flex-col gap-1 text-sm">
      <li>
        <Icon icon={EllipsisVerticalIcon} /> icons sit on the text line
      </li>
      <li>
        <Icon icon={UserIcon} /> in lists and menus
      </li>
      <li>
        <Icon icon={UnfoldHorizontalIcon} /> one size, one stroke width
      </li>
    </ul>
  ),
};

export const Picture: Story = {
  render: () => (
    <div className="flex items-end gap-6 text-xs">
      <div className="flex flex-col items-center gap-1">
        <Icon icon={CheckIcon} />
        default
      </div>
      <div className="flex flex-col items-center gap-1">
        <Icon icon={CheckIcon} className="size-10 text-green" />
        size-10, a picture
      </div>
    </div>
  ),
};

const stateIcons = [FolderIcon, FolderOpenIcon, UserIcon, SquareIcon];

const looks = [
  { label: "off", className: undefined, filled: false },
  { label: "filled", className: undefined, filled: true },
  { label: "tinted fill", className: "fill-current/25", filled: false },
  { label: "stroke-4", className: "stroke-4", filled: false },
];

export const State: Story = {
  render: () => (
    <div className="grid grid-cols-[auto_repeat(4,30px)] items-center gap-x-2 gap-y-1 text-xs text-primary-500">
      {looks.map((look) => (
        <div key={look.label} className="contents">
          <span className="text-gray-800">{look.label}</span>
          {stateIcons.map((icon) => (
            <Icon
              key={icon.displayName}
              icon={icon}
              filled={look.filled}
              className={look.className}
            />
          ))}
        </div>
      ))}
    </div>
  ),
};

export const InheritsColor: Story = {
  render: () => (
    <div className="flex items-center gap-6 text-sm">
      <span className="text-gray-500">
        <Icon icon={UserIcon} /> muted text
      </span>
      <span className="text-red">
        <Icon icon={TrashIcon} /> danger text
      </span>
      <Button intent="primary">
        <Icon icon={CheckIcon} /> inside a button
      </Button>
      <Button intent="secondary" danger>
        <Icon icon={TrashIcon} />
        red icon button
      </Button>
    </div>
  ),
};

export const Spinner: Story = {
  render: () => <Icon icon={LoaderCircleIcon} />,
};
