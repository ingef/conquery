import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";

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
    <button
      type="button"
      className="p-5 text-sm underline"
      onClick={() => setCount((c) => c + 1)}
    >
      {label}: clicked {count} times
    </button>
  );
};

/**
 * `shouldForceMount` keeps a panel mounted (hidden, inert) while another tab
 * shows, so what the user did in it survives a switch: the panes use it for
 * the concept trees and the editors. Click a counter, switch, switch back.
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

export const PrimarySmall: Story = {
  render: () => (
    <Tabs size="sm" defaultSelectedKey="all">
      <TabList aria-label="Filter">
        <Tab id="all">All</Tab>
        <Tab id="own">Own</Tab>
        <Tab id="shared">Shared</Tab>
      </TabList>
    </Tabs>
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
