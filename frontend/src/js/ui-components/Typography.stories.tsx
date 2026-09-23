import type { Meta, StoryObj } from "@storybook/react";

import { C1, C2, C3, H1, H2, H3, H4, H5 } from "./Typography";

export default {
  title: "UiComponents/Typography",
  component: C2,
  parameters: { layout: "padded" },
} as Meta<typeof C2>;

type Story = StoryObj<typeof C2>;

export const Scale: Story = {
  render: () => (
    <div className="flex flex-col gap-4">
      <H1>H1 – Population by region, 24 px</H1>
      <H2>H2 – Population by region, 20 px</H2>
      <H3>H3 – Population by region, 16 px</H3>
      <H4>H4 – Population by region, 14 px</H4>
      <H5>H5 – Population by region, 12 px</H5>
      <C1>
        C1 – The census counts every resident of a region once a decade, 16 px.
      </C1>
      <C2>
        C2 – The census counts every resident of a region once a decade, 14 px.
      </C2>
      <C3>
        C3 – The census counts every resident of a region once a decade, 12 px.
      </C3>
    </div>
  ),
};

export const Tones: Story = {
  render: () => (
    <div className="flex flex-col gap-2">
      <C2>default – inherits the color of its container</C2>
      <C2 tone="muted">muted – secondary information</C2>
      <C2 tone="danger">danger – errors and warnings</C2>
      <C2 tone="success">success – a finished step</C2>
      <C2 tone="primary">primary – the brand color, for selected things</C2>
      <C2 strong>strong – weight 500, the only emphasis weight</C2>
    </div>
  ),
};

export const Inline: Story = {
  render: () => (
    <C2>
      A sentence with{" "}
      <C2 as="span" strong>
        an emphasized part
      </C2>
      , a{" "}
      <C2 as="span" tone="muted">
        muted aside
      </C2>{" "}
      and <C3 as="span">a smaller note</C3> in one line.
    </C2>
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
