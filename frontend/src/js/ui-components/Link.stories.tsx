import type { Meta, StoryObj } from "@storybook/react";

import { Link } from "./Link";
import { C2 } from "./Typography";

export default {
  title: "UiComponents/Link",
  component: Link,
  parameters: { layout: "centered" },
} as Meta<typeof Link>;

type Story = StoryObj<typeof Link>;

export const Default: Story = {
  render: () => <Link href="#regions">Regions</Link>,
};

export const External: Story = {
  render: () => (
    <Link href="https://example.com" external>
      Open the manual
    </Link>
  ),
};

export const InText: Story = {
  render: () => (
    <C2>
      The census counts every resident once a decade, see the{" "}
      <Link href="https://example.com" external>
        method
      </Link>
      .
    </C2>
  ),
};

export const InButtonLooks: Story = {
  render: () => (
    <div className="flex items-center gap-3">
      <Link href="#regions" intent="primary">
        Primary
      </Link>
      <Link href="#regions" intent="secondary">
        Secondary
      </Link>
      <Link href="#regions" intent="tertiary">
        Tertiary
      </Link>
      <Link href="#regions" intent="secondary" danger>
        Danger
      </Link>
    </div>
  ),
};
