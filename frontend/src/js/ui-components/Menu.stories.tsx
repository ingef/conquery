import type { Meta, StoryObj } from "@storybook/react";
import {
  BookIcon,
  EllipsisVerticalIcon,
  InfoIcon,
  LogOutIcon,
  SendIcon,
  TrashIcon,
} from "lucide-react";
import { MenuTrigger } from "react-aria-components";
import { Button } from "./Button";
import { ConfirmMenu } from "./ConfirmMenu";
import { Menu, MenuItem, MenuSeparator } from "./Menu";
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
      <Button intent="secondary">
        <EllipsisVerticalIcon />
      </Button>
      <Menu aria-label="Actions" onAction={(key) => console.log(key)}>
        <MenuItem id="contact" href="mailto:someone@example.com">
          <SendIcon />A link item
        </MenuItem>
        <MenuItem id="manual">
          <BookIcon />
          An action item
        </MenuItem>
        <MenuItem id="disabled" isDisabled>
          <TrashIcon />A disabled item
        </MenuItem>
        <MenuItem id="delete" danger>
          <TrashIcon />A dangerous item
        </MenuItem>
      </Menu>
    </MenuTrigger>
  ),
};

export const WithSeparator: Story = {
  render: () => (
    <MenuTrigger>
      <Button intent="secondary">
        <EllipsisVerticalIcon />
      </Button>
      <Menu aria-label="Actions" onAction={(key) => console.log(key)}>
        <MenuItem id="manual">
          <BookIcon />
          Manual
        </MenuItem>
        <MenuItem id="version">
          <InfoIcon />
          Version
        </MenuItem>
        <MenuSeparator />
        <MenuItem id="logout">
          <LogOutIcon />
          Logout
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
        <Button intent="secondary">
          <TrashIcon />
        </Button>
      </ConfirmMenu>
    </div>
  ),
};

export const WithTooltipOnTrigger: Story = {
  render: () => (
    <TooltipTrigger>
      <ConfirmMenu confirmationText="Really delete?" onConfirm={() => {}}>
        <Button intent="secondary">
          <TrashIcon />
        </Button>
      </ConfirmMenu>
      <Tooltip>Delete</Tooltip>
    </TooltipTrigger>
  ),
};

const regions = ["North", "South", "East", "West", "Central"];
const manyItems = Array.from({ length: 200 }, (_, i) => ({
  id: `item-${i}`,
  label: `${regions[i % regions.length]} region ${i + 1}${
    i % 3 === 0 ? ", population and products per year" : ""
  }`,
}));

/** 200 items: every hover re-renders the whole list (react-aria, see Menu.tsx) */
export const ManyItems: Story = {
  render: () => (
    <MenuTrigger>
      <Button intent="secondary">200 items</Button>
      <Menu aria-label="Regions" onAction={(key) => console.log(key)}>
        {manyItems.map((item) => (
          <MenuItem key={item.id} id={item.id}>
            {item.label}
          </MenuItem>
        ))}
      </Menu>
    </MenuTrigger>
  ),
};
