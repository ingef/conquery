import {
  faArrowsLeftRightToLine,
  faCheck,
  faEllipsisV,
  faFolder,
  faPaperPlane,
  faSpinner,
  faTrash,
  faUser,
} from "@fortawesome/free-solid-svg-icons";
import type { Meta, StoryObj } from "@storybook/react";

import IconButton from "../button/IconButton";
import PrimaryButton from "../button/PrimaryButton";

import { Icon } from "./Icon";

export default {
  title: "UiComponents/Icon",
  component: Icon,
  parameters: { layout: "centered" },
} as Meta<typeof Icon>;

type Story = StoryObj<typeof Icon>;

const glyphs = [
  faEllipsisV,
  faUser,
  faFolder,
  faPaperPlane,
  faArrowsLeftRightToLine,
];

export const Frame: Story = {
  render: () => (
    <div className="flex flex-col gap-4 text-xs">
      <div className="flex items-center gap-2">
        {glyphs.map((icon, i) => (
          <Icon key={i} icon={icon} className="bg-gray-100" />
        ))}
        <span>narrow to wide glyphs, one 14 px frame each</span>
      </div>
      <ul className="flex flex-col gap-1">
        <li>
          <Icon icon={faEllipsisV} /> icons line up
        </li>
        <li>
          <Icon icon={faUser} /> in lists and menus
        </li>
        <li>
          <Icon icon={faArrowsLeftRightToLine} /> whatever their shape
        </li>
      </ul>
    </div>
  ),
};

export const Picture: Story = {
  render: () => (
    <div className="flex items-end gap-6 text-xs">
      <div className="flex flex-col items-center gap-1">
        <Icon icon={faCheck} />
        default
      </div>
      <div className="flex flex-col items-center gap-1">
        <Icon icon={faCheck} className="size-[30px] text-green" />
        size-[30px], a picture
      </div>
    </div>
  ),
};

export const InheritsColor: Story = {
  render: () => (
    <div className="flex items-center gap-6 text-sm">
      <span className="text-gray-500">
        <Icon icon={faUser} /> muted text
      </span>
      <span className="text-red">
        <Icon icon={faTrash} /> danger text
      </span>
      <PrimaryButton>
        <Icon icon={faCheck} /> inside a button
      </PrimaryButton>
      <IconButton frame icon={faTrash} red>
        red icon button
      </IconButton>
    </div>
  ),
};

export const Spinner: Story = {
  render: () => <Icon icon={faSpinner} />,
};
