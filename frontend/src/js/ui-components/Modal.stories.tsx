import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";
import { DialogTrigger, MenuTrigger } from "react-aria-components";

import type { SelectOptionT } from "../api/types";
import { Button } from "./Button";
import { ComboBoxField } from "./ComboBoxField";
import { DateField } from "./DateField/DateField";
import InputMultiSelect from "./InputMultiSelect/InputMultiSelect";
import { Menu, MenuItem } from "./Menu";
import { Modal, ModalBody, ModalFooter, ModalHeader } from "./Modal";
import { Tooltip, TooltipTrigger, tooltipDelay } from "./Tooltip";

export default {
  title: "UiComponents/Modal",
  component: Modal,
  parameters: { layout: "centered" },
} as Meta<typeof Modal>;

type Story = StoryObj<typeof Modal>;

export const Confirmation: Story = {
  render: () => (
    <DialogTrigger>
      <Button>Delete folder</Button>
      <Modal size="sm">
        <ModalHeader>Delete the folder?</ModalHeader>
        <ModalBody>
          <p>
            The folder "Reports 2024" is removed from all queries. The queries
            themselves stay.
          </p>
        </ModalBody>
        <ModalFooter>
          <Button slot="close">Cancel</Button>
          <Button slot="close" danger>
            Delete
          </Button>
        </ModalFooter>
      </Modal>
    </DialogTrigger>
  ),
};

export const HeaderAndBodyOnly: Story = {
  render: () => (
    <DialogTrigger>
      <Button>Settings</Button>
      <Modal>
        <ModalHeader subtitle="Applies to this session">Settings</ModalHeader>
        <ModalBody>
          <p>
            Escape or a click outside closes the dialog; there is no footer.
          </p>
        </ModalBody>
      </Modal>
    </DialogTrigger>
  ),
};

export const DoneButton: Story = {
  render: () => (
    <DialogTrigger>
      <Button>Edit dates</Button>
      <Modal>
        <ModalHeader>Date range</ModalHeader>
        <ModalBody>
          <p>Edits apply as you make them, so the footer only has "Done".</p>
        </ModalBody>
        <ModalFooter>
          <Button slot="close">Done</Button>
        </ModalFooter>
      </Modal>
    </DialogTrigger>
  ),
};

export const WithForm: Story = {
  render: () => (
    <DialogTrigger>
      <Button>New folder</Button>
      <Modal>
        {({ close }) => (
          <>
            <ModalHeader>New folder</ModalHeader>
            <form
              className="flex flex-col gap-5"
              onSubmit={(e) => {
                e.preventDefault();
                close();
              }}
            >
              <ModalBody>
                <label className="flex flex-col gap-1 text-sm">
                  Name
                  <input className="rounded border border-gray-500 px-2 h-[30px]" />
                </label>
              </ModalBody>
              <ModalFooter>
                <Button slot="close">Cancel</Button>
                <Button intent="primary" type="submit">
                  Create
                </Button>
              </ModalFooter>
            </form>
          </>
        )}
      </Modal>
    </DialogTrigger>
  ),
};

const sizes = ["sm", "md", "lg", "xl", "full"] as const;

export const Sizes: Story = {
  render: () => (
    <div className="flex gap-2">
      {sizes.map((size) => (
        <DialogTrigger key={size}>
          <Button>{size}</Button>
          <Modal size={size}>
            <ModalHeader>Size {size}</ModalHeader>
            <ModalBody>
              <p>
                The card has a fixed width per size; the content fills it and
                wraps inside it.
              </p>
            </ModalBody>
            <ModalFooter>
              <Button slot="close">Done</Button>
            </ModalFooter>
          </Modal>
        </DialogTrigger>
      ))}
    </div>
  ),
};

export const Scrollable: Story = {
  render: () => (
    <DialogTrigger>
      <Button>Long content</Button>
      <Modal scrollable>
        <ModalHeader>Regions</ModalHeader>
        <ModalBody>
          <ul className="flex flex-col gap-2">
            {Array.from({ length: 60 }, (_, i) => (
              <li key={i}>Region {i + 1}</li>
            ))}
          </ul>
        </ModalBody>
        <ModalFooter>
          <Button slot="close">Done</Button>
        </ModalFooter>
      </Modal>
    </DialogTrigger>
  ),
};

const Controlled = () => {
  const [isOpen, setOpen] = useState(false);
  return (
    <div className="flex flex-col items-start gap-2 text-sm">
      <span>Opened from somewhere else than a trigger button:</span>
      <Button onPress={() => setOpen(true)}>Simulate a file drop</Button>
      <Modal isOpen={isOpen} onOpenChange={setOpen}>
        <ModalHeader>File received</ModalHeader>
        <ModalBody>
          <p>3 of 4 rows resolved.</p>
        </ModalBody>
        <ModalFooter>
          <Button slot="close">Done</Button>
          <Button slot="close" intent="primary">
            Insert
          </Button>
        </ModalFooter>
      </Modal>
    </div>
  );
};

/** Controlled with `isOpen` / `onOpenChange` when the trigger is not a button next to it. */
export const ControlledOpenState: Story = { render: () => <Controlled /> };

const REGIONS: SelectOptionT[] = Array.from({ length: 60 }, (_, i) => ({
  value: i,
  label: `Region ${i + 1}`,
}));

// every kind of overlay that can open from inside a dialog, at the bottom of the card
const Overlays = () => {
  const [region, setRegion] = useState<SelectOptionT | null>(null);
  const [regions, setRegions] = useState<SelectOptionT[]>([]);
  const [date, setDate] = useState("");

  return (
    <div className="flex flex-col gap-3">
      <ComboBoxField
        label="Region"
        options={REGIONS}
        value={region}
        onChange={setRegion}
      />
      <InputMultiSelect
        label="Regions"
        options={REGIONS}
        value={regions}
        onChange={setRegions}
      />
      <DateField
        label="Start"
        value={date}
        onChange={setDate}
        dateFormat="dd.MM.yyyy"
        placeholder="DD.MM.YYYY"
      />
      <div className="flex gap-2">
        <MenuTrigger>
          <Button>Menu</Button>
          <Menu aria-label="Regions">
            {REGIONS.slice(0, 30).map((r) => (
              <MenuItem key={r.value} id={r.value}>
                {r.label}
              </MenuItem>
            ))}
          </Menu>
        </MenuTrigger>
        <TooltipTrigger delay={tooltipDelay.immediate}>
          <Button>Tooltip</Button>
          <Tooltip>
            A tooltip that must not be cut off by the card either.
          </Tooltip>
        </TooltipTrigger>
      </div>
    </div>
  );
};

/** The combobox list, menu, calendar and tooltip open in portals; the multi select's list does not. */
export const OverlaysInside: Story = {
  render: () => (
    <DialogTrigger>
      <Button>Overlays</Button>
      <Modal>
        <ModalHeader>Overlays inside a dialog</ModalHeader>
        <ModalBody>
          <Overlays />
        </ModalBody>
        <ModalFooter>
          <Button slot="close">Done</Button>
        </ModalFooter>
      </Modal>
    </DialogTrigger>
  ),
};

/** Same, in a card that scrolls: a list that is not portaled gets clipped here. */
export const OverlaysInsideScrollable: Story = {
  render: () => (
    <DialogTrigger>
      <Button>Overlays, scrolling card</Button>
      <Modal scrollable>
        <ModalHeader>Overlays inside a scrolling dialog</ModalHeader>
        <ModalBody>
          <ul className="mb-5 flex flex-col gap-2">
            {Array.from({ length: 40 }, (_, i) => (
              <li key={i}>Row {i + 1}</li>
            ))}
          </ul>
          <Overlays />
        </ModalBody>
        <ModalFooter>
          <Button slot="close">Done</Button>
        </ModalFooter>
      </Modal>
    </DialogTrigger>
  ),
};
