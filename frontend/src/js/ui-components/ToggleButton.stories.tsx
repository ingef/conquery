import type { Meta, StoryObj } from "@storybook/react";
import {
  BanIcon,
  CalendarIcon,
  FolderIcon,
  PinIcon,
  SquarePenIcon,
} from "lucide-react";
import { useState } from "react";
import { DialogTrigger } from "react-aria-components";

import { Button } from "./Button";
import { CheckboxField } from "./CheckboxField";
import { Modal, ModalBody, ModalFooter, ModalHeader } from "./Modal";
import { ToggleButton, type ToggleButtonProps } from "./ToggleButton";
import { Tooltip, TooltipTrigger } from "./Tooltip";

export default {
  title: "UiComponents/ToggleButton",
  component: ToggleButton,
  parameters: { layout: "centered" },
} as Meta<typeof ToggleButton>;

type Story = StoryObj<typeof ToggleButton>;

// each example keeps its own state so it can be switched in the story
const Toggle = ({
  children,
  defaultSelected = false,
  ...props
}: Omit<ToggleButtonProps, "isSelected" | "onChange"> & {
  defaultSelected?: boolean;
}) => {
  const [selected, setSelected] = useState(defaultSelected);
  return (
    <ToggleButton isSelected={selected} onChange={setSelected} {...props}>
      {children}
    </ToggleButton>
  );
};

const Row = ({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) => (
  <div className="flex items-center gap-3">
    <span className="w-40 text-xs text-gray-500">{label}</span>
    {children}
  </div>
);

export const Intents: Story = {
  render: () => (
    <div className="flex flex-col gap-3">
      <Row label="tertiary (default), off / on">
        <Toggle>
          <FolderIcon />
          folders
        </Toggle>
        <Toggle defaultSelected>
          <FolderIcon />
          folders
        </Toggle>
      </Row>
      <Row label="secondary, off / on">
        <Toggle intent="secondary">
          <FolderIcon />
          folders
        </Toggle>
        <Toggle intent="secondary" defaultSelected>
          <FolderIcon />
          folders
        </Toggle>
      </Row>
    </div>
  ),
};

export const Highlights: Story = {
  render: () => (
    <div className="flex flex-col gap-3">
      <Row label="primary: a setting is on">
        <Toggle defaultSelected>
          <CalendarIcon />
          date
        </Toggle>
      </Row>
      <Row label="danger: a warning state is on">
        <Toggle highlight="danger" defaultSelected>
          <BanIcon />
          exclude
        </Toggle>
      </Row>
    </div>
  ),
};

export const Sizes: Story = {
  render: () => (
    <div className="flex flex-col gap-3">
      {(["sm", "md", "lg"] as const).map((size) => (
        <Row key={size} label={size}>
          <Toggle size={size} defaultSelected>
            <SquarePenIcon />
            edit
          </Toggle>
          <Toggle size={size} intent="secondary" defaultSelected>
            <SquarePenIcon />
            edit
          </Toggle>
          <Toggle size={size} defaultSelected aria-label="Pin">
            <PinIcon />
          </Toggle>
        </Row>
      ))}
    </div>
  ),
};

export const IconOnlyWithTooltip: Story = {
  render: () => (
    <Row label="square, named by the tooltip">
      <TooltipTrigger>
        <Toggle defaultSelected aria-label="Pin">
          <PinIcon />
        </Toggle>
        <Tooltip>Pin</Tooltip>
      </TooltipTrigger>
    </Row>
  ),
};

const RestrictionEditor = () => {
  const [years, setYears] = useState({ before2020: false, after2024: false });
  const restricted = years.before2020 || years.after2024;
  return (
    <Row label="pressing opens an editor">
      <DialogTrigger>
        <ToggleButton isSelected={restricted}>
          <CalendarIcon />
          {restricted ? "restricted" : "all years"}
        </ToggleButton>
        <Modal size="sm">
          <ModalHeader>Exclude years</ModalHeader>
          <ModalBody>
            <div className="flex flex-col gap-1">
              <CheckboxField
                isSelected={years.before2020}
                onChange={(before2020) => setYears({ ...years, before2020 })}
              >
                before 2020
              </CheckboxField>
              <CheckboxField
                isSelected={years.after2024}
                onChange={(after2024) => setYears({ ...years, after2024 })}
              >
                after 2024
              </CheckboxField>
            </div>
          </ModalBody>
          <ModalFooter>
            <Button slot="close">Done</Button>
          </ModalFooter>
        </Modal>
      </DialogTrigger>
    </Row>
  );
};

/**
 * A button whose look reflects a state is a ToggleButton even when pressing
 * opens an editor: the selected look mirrors the state, the modal edits it.
 */
export const OpensAnEditor: Story = { render: () => <RestrictionEditor /> };
