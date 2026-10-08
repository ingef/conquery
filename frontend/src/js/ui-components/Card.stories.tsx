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

export const WithoutPadding: Story = {
  render: () => (
    <Card padding="none">
      <div className="border-gray-100 border-b px-[14px] py-2">
        <C2 strong>Year</C2>
      </div>
      <div className="px-[14px] py-2">
        <C2>2024</C2>
      </div>
      <div className="border-gray-100 border-t px-[14px] py-2">
        <C2>2025</C2>
      </div>
    </Card>
  ),
};
