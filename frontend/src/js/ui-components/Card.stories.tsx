import type { Meta, StoryObj } from "@storybook/react";

import { Card } from "./Card";
import { C2, C3, H3 } from "./Typography";

export default {
  title: "UiComponents/Card",
  component: Card,
  decorators: [
    (Story) => (
      <div className="w-[400px] bg-bg-100 p-5">
        <Story />
      </div>
    ),
  ],
} as Meta<typeof Card>;

type Story = StoryObj<typeof Card>;

export const WithHeading: Story = {
  render: () => (
    <Card>
      <H3>Regions</H3>
      <C3 tone="muted">12 entries</C3>
      <div className="mt-4">
        <C2>North, South, East and West, with their population per year.</C2>
      </div>
    </Card>
  ),
};

export const Stacked: Story = {
  render: () => (
    <div className="flex flex-col gap-[10px]">
      <Card>
        <C2>Population</C2>
      </Card>
      <Card>
        <C2>Products</C2>
      </Card>
    </div>
  ),
};
