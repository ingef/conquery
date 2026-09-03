import { faEllipsisV, faTrash } from "@fortawesome/free-solid-svg-icons";
import type { Meta, StoryObj } from "@storybook/react";
import { DialogTrigger } from "react-aria-components";

import IconButton from "../button/IconButton";
import { TransparentButton } from "../button/TransparentButton";

import { ConfirmPopover } from "./ConfirmPopover";
import { Dialog, Popover } from "./Popover";
import { Tooltip, TooltipTrigger } from "./Tooltip";

export default {
  title: "UiComponents/Popover",
  component: Popover,
  parameters: { layout: "centered" },
} as Meta<typeof Popover>;

type Story = StoryObj<typeof Popover>;

export const Default: Story = {
  render: () => (
    <DialogTrigger>
      <IconButton frame icon={faEllipsisV} />
      <Popover>
        <Dialog aria-label="Actions">
          {({ close }) => (
            <div className="flex flex-col gap-[2px] p-2">
              <IconButton
                className="w-full"
                bgHover
                icon={faTrash}
                onClick={close}
              >
                First action
              </IconButton>
              <IconButton
                className="w-full"
                bgHover
                icon={faTrash}
                onClick={close}
              >
                Second action
              </IconButton>
            </div>
          )}
        </Dialog>
      </Popover>
    </DialogTrigger>
  ),
};

export const Confirm: Story = {
  render: () => (
    <div className="flex items-center gap-4">
      <ConfirmPopover confirmationText="Really clear?" onConfirm={() => {}}>
        <TransparentButton>Clear</TransparentButton>
      </ConfirmPopover>
      <ConfirmPopover
        red
        placement="top"
        confirmationText="Delete for good"
        onConfirm={() => {}}
      >
        <IconButton frame icon={faTrash} />
      </ConfirmPopover>
    </div>
  ),
};

export const WithTooltipOnTrigger: Story = {
  render: () => (
    <TooltipTrigger>
      <ConfirmPopover confirmationText="Really delete?" onConfirm={() => {}}>
        <IconButton frame icon={faTrash} />
      </ConfirmPopover>
      <Tooltip>Delete</Tooltip>
    </TooltipTrigger>
  ),
};
