import {
  faBook,
  faEllipsisV,
  faPaperPlane,
  faTrash,
} from "@fortawesome/free-solid-svg-icons";
import type { Meta, StoryObj } from "@storybook/react";
import { MenuTrigger } from "react-aria-components";
import IconButton from "../button/IconButton";
import { Button } from "./Button";
import { ConfirmMenu } from "./ConfirmMenu";
import { Icon } from "./Icon";
import { Menu, MenuItem } from "./Menu";
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
          <Icon icon={faPaperPlane} />A link item
        </MenuItem>
        <MenuItem id="manual">
          <Icon icon={faBook} />
          An action item
        </MenuItem>
        <MenuItem id="disabled" isDisabled>
          <Icon icon={faTrash} />A disabled item
        </MenuItem>
        <MenuItem id="delete" danger>
          <Icon icon={faTrash} />A dangerous item
        </MenuItem>
      </Menu>
    </MenuTrigger>
  ),
};

export const Confirm: Story = {
  render: () => (
    <div className="flex items-center gap-4">
      <ConfirmMenu confirmationText="Really clear?" onConfirm={() => {}}>
        <Button intent="secondary">Clear</Button>
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
