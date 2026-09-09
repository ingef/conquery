import type { Meta, StoryObj } from "@storybook/react";

import { Tab, TabList, TabPanel, Tabs } from "./Tabs";

export default {
  title: "UiComponents/Tabs",
  component: Tabs,
  parameters: { layout: "centered" },
} as Meta<typeof Tabs>;

type Story = StoryObj<typeof Tabs>;

export const Underline: Story = {
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

export const UnderlineSmall: Story = {
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

export const Boxed: Story = {
  render: () => (
    <div className="w-[400px]">
      <Tabs variant="boxed" defaultSelectedKey="north">
        <TabList aria-label="Region">
          <Tab id="north" tooltip="The northern regions">
            North
          </Tab>
          <Tab id="south">South</Tab>
        </TabList>
        <div className="rounded border border-gray-500 bg-bg-50 px-[10px] py-3 text-sm">
          <TabPanel id="north">Fields for the north.</TabPanel>
          <TabPanel id="south">Fields for the south.</TabPanel>
        </div>
      </Tabs>
    </div>
  ),
};
