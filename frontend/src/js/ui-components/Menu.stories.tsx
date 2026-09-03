import {
  faBook,
  faEllipsisV,
  faPaperPlane,
  faTrash,
} from "@fortawesome/free-solid-svg-icons";
import type { Meta, StoryObj } from "@storybook/react";
import { MenuTrigger } from "react-aria-components";

import IconButton from "../button/IconButton";
import { TransparentButton } from "../button/TransparentButton";
import FaIcon from "../icon/FaIcon";

import { ConfirmMenu } from "./ConfirmMenu";
import { Menu, MenuItem, menuItemIcon } from "./Menu";
import { Tooltip, TooltipTrigger } from "./Tooltip";

export default {
  title: "UiComponents/Menu",
  component: Menu,
  parameters: { layout: "centered" },
} as Meta<typeof Menu>;

type Story = StoryObj<typeof Menu>;

export const Default: Story = {
  render: () => (
    <MenuTrigger>
      <IconButton frame icon={faEllipsisV} />
      <Menu aria-label="Actions" onAction={(key) => console.log(key)}>
        <MenuItem id="contact" href="mailto:someone@example.com">
          <span className={menuItemIcon()}>
            <FaIcon icon={faPaperPlane} />
          </span>
          A link item
        </MenuItem>
        <MenuItem id="manual">
          <span className={menuItemIcon()}>
            <FaIcon icon={faBook} />
          </span>
          An action item
        </MenuItem>
        <MenuItem id="disabled" isDisabled>
          <span className={menuItemIcon()}>
            <FaIcon icon={faTrash} />
          </span>
          A disabled item
        </MenuItem>
        <MenuItem id="delete" danger>
          <span className={menuItemIcon()}>
            <FaIcon icon={faTrash} />
          </span>
          A dangerous item
        </MenuItem>
      </Menu>
    </MenuTrigger>
  ),
};

export const Confirm: Story = {
  render: () => (
    <div className="flex items-center gap-4">
      <ConfirmMenu confirmationText="Really clear?" onConfirm={() => {}}>
        <TransparentButton>Clear</TransparentButton>
      </ConfirmMenu>
      <ConfirmMenu
        red
        placement="top"
        confirmationText="Delete for good"
        onConfirm={() => {}}
      >
        <IconButton frame icon={faTrash} />
      </ConfirmMenu>
    </div>
  ),
};

export const WithTooltipOnTrigger: Story = {
  render: () => (
    <TooltipTrigger>
      <ConfirmMenu confirmationText="Really delete?" onConfirm={() => {}}>
        <IconButton frame icon={faTrash} />
      </ConfirmMenu>
      <Tooltip>Delete</Tooltip>
    </TooltipTrigger>
  ),
};
