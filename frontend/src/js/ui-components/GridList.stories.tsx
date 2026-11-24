import type { Meta, StoryObj } from "@storybook/react";
import { FunnelIcon } from "lucide-react";
import { useState } from "react";
import type { Key } from "react-aria-components";

import { CheckboxField } from "./CheckboxField";
import { GridList, GridListItem } from "./GridList";
import { ToggleButton } from "./ToggleButton";
import { C2 } from "./Typography";

export default {
  title: "UiComponents/GridList",
  component: GridList,
} as Meta<typeof GridList>;

type Story = StoryObj<typeof GridList>;

const regions = ["North", "East", "South", "West"];

export const SingleSelection: Story = {
  render: () => {
    const [selected, setSelected] = useState<Key>("North");
    return (
      <div className="w-64">
        <GridList
          aria-label="Regions"
          selectionMode="single"
          disallowEmptySelection
          selectedKeys={[selected]}
          onSelectionChange={(keys) => {
            if (keys !== "all") setSelected([...keys][0]);
          }}
        >
          {regions.map((region) => (
            <GridListItem key={region} id={region} textValue={region}>
              <C2 truncate>{region}</C2>
            </GridListItem>
          ))}
        </GridList>
      </div>
    );
  },
};

export const WithControls: Story = {
  render: () => {
    const [selected, setSelected] = useState<Key>("North");
    const [included, setIncluded] = useState<string[]>(["North", "East"]);
    const [filtered, setFiltered] = useState<string[]>(["North"]);
    return (
      <div className="w-72">
        <GridList
          aria-label="Regions"
          selectionMode="single"
          disallowEmptySelection
          selectedKeys={[selected]}
          onSelectionChange={(keys) => {
            if (keys !== "all") setSelected([...keys][0]);
          }}
        >
          {regions.map((region) => (
            <GridListItem key={region} id={region} textValue={region}>
              <CheckboxField
                aria-label={`Include ${region}`}
                isSelected={included.includes(region)}
                onChange={(include) =>
                  setIncluded((prev) =>
                    include
                      ? [...prev, region]
                      : prev.filter((r) => r !== region),
                  )
                }
              />
              <div className="min-w-0 grow">
                <C2
                  truncate
                  tone={included.includes(region) ? undefined : "muted"}
                >
                  {region}
                </C2>
              </div>
              <span className="flex size-6 shrink-0 items-center justify-center">
                {filtered.includes(region) && (
                  <ToggleButton
                    aria-label={`Clear the filters of ${region}`}
                    size="sm"
                    isSelected
                    onChange={() =>
                      setFiltered((prev) => prev.filter((r) => r !== region))
                    }
                  >
                    <FunnelIcon />
                  </ToggleButton>
                )}
              </span>
            </GridListItem>
          ))}
        </GridList>
      </div>
    );
  },
};
