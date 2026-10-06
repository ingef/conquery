import type { Meta, StoryObj } from "@storybook/react";
import { Fragment } from "react";

import { C1, C2, C3, H1, H2, H3, H4, H5 } from "./Typography";

export default {
  title: "UiComponents/Typography",
  component: C2,
  parameters: { layout: "padded" },
} as Meta<typeof C2>;

type Story = StoryObj<typeof C2>;

export const Headings: Story = {
  render: () => (
    <div className="flex flex-col gap-4">
      <H1>H1 – Population by region, 24 px</H1>
      <H2>H2 – Population by region, 20 px</H2>
      <H3>H3 – Population by region, 16 px</H3>
      <H4>H4 – Population by region, 14 px</H4>
      <H5>H5 – Population by region, 12 px</H5>
    </div>
  ),
};

const tones = ["default", "muted", "danger", "success", "primary"] as const;
const sizes = [
  { Component: C1, name: "C1, 16 px" },
  { Component: C2, name: "C2, 14 px" },
  { Component: C3, name: "C3, 12 px" },
];

export const Content: Story = {
  render: () => (
    <div className="grid grid-cols-[auto_repeat(6,auto)] items-baseline gap-x-6 gap-y-3">
      <span />
      {tones.map((tone) => (
        <C3 key={tone} tone="muted">
          {tone}
        </C3>
      ))}
      <C3 tone="muted">strong</C3>
      {sizes.map(({ Component, name }) => (
        <Fragment key={name}>
          <C3 tone="muted">{name}</C3>
          {tones.map((tone) => (
            <Component key={tone} tone={tone}>
              Population by region
            </Component>
          ))}
          <Component strong>Population by region</Component>
        </Fragment>
      ))}
    </div>
  ),
};

export const Truncate: Story = {
  render: () => (
    <div className="flex w-64 flex-col gap-2 rounded border border-gray-100 p-2">
      <H4 truncate>A heading that is far too long for its 256 px container</H4>
      <C2 truncate>
        A line of text that is far too long for its 256 px container
      </C2>
    </div>
  ),
};

export const Composed: Story = {
  render: () => (
    <div className="flex w-96 flex-col gap-1">
      <H3>Regions</H3>
      <C2>
        Pick the regions whose residents count. Nested regions add up to their
        parent.
      </C2>
      <C3 tone="muted">Updated yesterday · 16 regions</C3>
    </div>
  ),
};
