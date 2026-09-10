import { faTimes } from "@fortawesome/free-solid-svg-icons";
import type { Meta, StoryObj } from "@storybook/react";

import { Button } from "./Button";
import {
  Disclosure,
  DisclosureGroup,
  DisclosurePanel,
  DisclosureTitle,
} from "./Disclosure";
import { Icon } from "./Icon";

export default {
  title: "UiComponents/Disclosure",
  component: Disclosure,
  parameters: { layout: "centered" },
} as Meta<typeof Disclosure>;

type Story = StoryObj<typeof Disclosure>;

export const Default: Story = {
  render: () => (
    <div className="w-[400px]">
      <Disclosure defaultExpanded>
        <DisclosureTitle>Time range</DisclosureTitle>
        <DisclosurePanel>
          <p className="text-sm">From 2020 to 2024, whole years.</p>
        </DisclosurePanel>
      </Disclosure>
    </div>
  ),
};

export const WithInfoAndActions: Story = {
  render: () => (
    <div className="w-[400px]">
      <Disclosure>
        <DisclosureTitle
          info="Regions the report covers."
          actions={
            <Button size="sm" intent="tertiary" aria-label="Remove">
              <Icon icon={faTimes} />
            </Button>
          }
        >
          Regions
        </DisclosureTitle>
        <DisclosurePanel>
          <p className="text-sm">North, South.</p>
        </DisclosurePanel>
      </Disclosure>
    </div>
  ),
};

const sections = ["Regions", "Years", "Products"];

export const Group: Story = {
  render: () => (
    <div className="w-[400px]">
      <DisclosureGroup defaultExpandedKeys={["Regions"]}>
        {sections.map((section) => (
          <Disclosure key={section} id={section}>
            <DisclosureTitle>{section}</DisclosureTitle>
            <DisclosurePanel>
              <p className="text-sm">Fields for {section.toLowerCase()}.</p>
            </DisclosurePanel>
          </Disclosure>
        ))}
      </DisclosureGroup>
    </div>
  ),
};

/** Several sections open at once. */
export const GroupMultiple: Story = {
  render: () => (
    <div className="w-[400px]">
      <DisclosureGroup allowsMultipleExpanded defaultExpandedKeys={sections}>
        {sections.map((section) => (
          <Disclosure key={section} id={section}>
            <DisclosureTitle>{section}</DisclosureTitle>
            <DisclosurePanel>
              <p className="text-sm">Fields for {section.toLowerCase()}.</p>
            </DisclosurePanel>
          </Disclosure>
        ))}
      </DisclosureGroup>
    </div>
  ),
};
