import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";
import { DialogTrigger } from "react-aria-components";

import { Button } from "./Button";
import { Modal, ModalBody, ModalFooter, ModalHeader } from "./Modal";

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
      <Modal>
        <ModalHeader>Delete the folder?</ModalHeader>
        <ModalBody>
          <p className="max-w-[400px]">
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
          <p className="w-[300px]">
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
          <p className="w-[350px]">
            Edits apply as you make them, so the footer only has "Done".
          </p>
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

export const Scrollable: Story = {
  render: () => (
    <DialogTrigger>
      <Button>Long content</Button>
      <Modal scrollable>
        <ModalHeader>Regions</ModalHeader>
        <ModalBody>
          <ul className="flex w-[300px] flex-col gap-2">
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
          <p className="w-[300px]">3 of 4 rows resolved.</p>
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
