import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";
import { useDrag } from "react-dnd";

import { DNDType } from "../common/constants/dndTypes";
import { Button } from "./Button";
import { Tab, TabList, TabPanel, Tabs } from "./Tabs";

export default {
  title: "UiComponents/Tabs",
  component: Tabs,
  parameters: { layout: "centered" },
} as Meta<typeof Tabs>;

type Story = StoryObj<typeof Tabs>;

export const Primary: Story = {
  render: () => (
    <div className="w-[480px]">
      <Tabs defaultSelectedKey="regions">
        <TabList aria-label="Sections">
          <Tab id="regions" tooltip="Regions and their population">
            Regions
          </Tab>
          <Tab id="years">Years</Tab>
          <Tab id="products">Products</Tab>
        </TabList>
        <TabPanel id="regions">
          <p className="p-5 text-sm">Population per region.</p>
        </TabPanel>
        <TabPanel id="years">
          <p className="p-5 text-sm">Population per year.</p>
        </TabPanel>
        <TabPanel id="products">
          <p className="p-5 text-sm">Products per region.</p>
        </TabPanel>
      </Tabs>
    </div>
  ),
};

const Counter = ({ label }: { label: string }) => {
  const [count, setCount] = useState(0);
  return (
    <div className="flex flex-col items-start gap-3 p-5 text-sm">
      <Button intent="primary" onPress={() => setCount((c) => c + 1)}>
        Count up in {label}
      </Button>
      <p>
        {label} counted {count} times. Switch to the other tab and back: the
        panel stays mounted, so the count survives.
      </p>
    </div>
  );
};

/**
 * Without `shouldForceMount` a panel unmounts while another tab shows and
 * loses its state. With it, the panel stays mounted, hidden and inert, which
 * is how the concept trees and the editors keep their state in the panes.
 */
export const ForceMountedPanels: Story = {
  render: () => (
    <div className="w-[480px]">
      <Tabs defaultSelectedKey="a">
        <TabList aria-label="Editors">
          <Tab id="a">Editor A</Tab>
          <Tab id="b">Editor B</Tab>
        </TabList>
        <TabPanel id="a" shouldForceMount>
          <Counter label="A" />
        </TabPanel>
        <TabPanel id="b" shouldForceMount>
          <Counter label="B" />
        </TabPanel>
      </Tabs>
    </div>
  ),
};

const Draggable = () => {
  const [, drag] = useDrag({
    type: DNDType.CONCEPT_TREE_NODE,
    item: { type: DNDType.CONCEPT_TREE_NODE },
  });
  return (
    <div
      ref={(el) => {
        drag(el);
      }}
      className="cursor-grab self-start rounded border border-gray-500 bg-white px-2 py-1 text-sm"
    >
      Drag me over a tab and hold
    </div>
  );
};

/** A dragged item hovering over a tab for a moment switches to that tab. */
export const SwitchWhileDragging: Story = {
  render: () => (
    <div className="flex w-[480px] flex-col gap-5">
      <Draggable />
      <Tabs defaultSelectedKey="regions">
        <TabList aria-label="Sections">
          <Tab id="regions">Regions</Tab>
          <Tab id="years">Years</Tab>
        </TabList>
        <TabPanel id="regions">
          <p className="p-5 text-sm">Drop targets for regions.</p>
        </TabPanel>
        <TabPanel id="years">
          <p className="p-5 text-sm">Drop targets for years.</p>
        </TabPanel>
      </Tabs>
    </div>
  ),
};

export const Secondary: Story = {
  render: () => (
    <div className="w-[400px]">
      <Tabs variant="secondary" defaultSelectedKey="north">
        <TabList aria-label="Region">
          <Tab id="north" tooltip="The northern regions">
            North
          </Tab>
          <Tab id="south">South</Tab>
        </TabList>
        <TabPanel id="north">
          <div className="rounded border border-gray-500 bg-bg-50 px-[10px] py-3 text-sm">
            Fields for the north.
          </div>
        </TabPanel>
        <TabPanel id="south">
          <div className="rounded border border-gray-500 bg-bg-50 px-[10px] py-3 text-sm">
            Fields for the south.
          </div>
        </TabPanel>
      </Tabs>
    </div>
  ),
};
