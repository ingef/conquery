import type { Meta, StoryObj } from "@storybook/react";
import { useRef, useState } from "react";

import type { SelectOptionT } from "../api/types";
import {
  ComboBoxMultiField,
  type ComboBoxMultiFieldProps,
} from "./ComboBoxMultiField";
import { withoutDuplicates } from "./ComboBoxParts";

export default {
  title: "FormComponents/ComboBoxMultiField",
  component: ComboBoxMultiField,
  parameters: { layout: "centered" },
} as Meta<typeof ComboBoxMultiField>;

type Story = StoryObj<typeof ComboBoxMultiField>;

const REGIONS: SelectOptionT[] = [
  { value: "north", label: "North" },
  { value: "east", label: "East" },
  { value: "south", label: "South" },
  { value: "west", label: "West" },
  { value: "central", label: "Central", disabled: true },
  { value: "islands", label: "**Islands** – seasonal" },
];

// each example keeps its own selection and shows what is stored
const Stateful = ({
  defaultValue = [],
  ...props
}: Omit<
  Extract<ComboBoxMultiFieldProps, { label: string }>,
  "value" | "onChange"
> & {
  defaultValue?: SelectOptionT[];
}) => {
  const [value, setValue] = useState<SelectOptionT[]>(defaultValue);
  return (
    <div className="flex w-96 flex-col">
      <ComboBoxMultiField value={value} onChange={setValue} {...props} />
      <span className="mt-10 text-xs text-gray-400">
        value: {JSON.stringify(value.map((item) => item.value))}
      </span>
    </div>
  );
};

export const Default: Story = {
  render: () => (
    <Stateful
      label="Regions"
      tooltip="Type to filter; a picked option stays in the list and a second pick removes it. Central cannot be picked."
      options={REGIONS}
      defaultValue={[REGIONS[0]]}
    />
  ),
};

export const Creatable: Story = {
  render: () => (
    <Stateful
      label="Folders"
      tooltip="Text that matches no folder becomes a new one with Enter."
      placeholder="Folder …"
      creatable
      options={[
        { value: "reports", label: "Reports" },
        { value: "drafts", label: "Drafts" },
      ]}
      defaultValue={[{ value: "reports", label: "Reports" }]}
    />
  ),
};

const ALL_REGIONS: SelectOptionT[] = Array.from({ length: 230 }, (_, i) => ({
  value: `region-${i + 1}`,
  label: `Region ${i + 1}`,
}));
const PAGE_SIZE = 25;

// a server search: pages of 25 arrive after a delay, the total counts every match
const ServerSearch = () => {
  const [value, setValue] = useState<SelectOptionT[]>([]);
  const [options, setOptions] = useState<SelectOptionT[]>([]);
  const [total, setTotal] = useState<number>();
  const [loading, setLoading] = useState(false);
  const loaded = useRef(0);

  const matches = (query: string) =>
    ALL_REGIONS.filter((option) =>
      option.label.toLowerCase().includes(query.toLowerCase()),
    );

  const load = (query: string, config?: { shouldReset?: boolean }) => {
    const from = config?.shouldReset ? 0 : loaded.current;
    const found = matches(query);
    if (from >= found.length && !config?.shouldReset) return;

    setLoading(true);
    setTimeout(() => {
      const page = found.slice(from, from + PAGE_SIZE);
      loaded.current = from + page.length;
      setOptions((previous) =>
        config?.shouldReset ? page : withoutDuplicates(previous, page),
      );
      setTotal(found.length);
      setLoading(false);
    }, 400);
  };

  return (
    <div className="flex w-96 flex-col">
      <ComboBoxMultiField
        label="Regions"
        tooltip="230 regions on a server: the list loads 25 at a time, typing searches on the server, insert all takes every match."
        options={options}
        value={value}
        onChange={setValue}
        total={total}
        loading={loading}
        onLoadMore={load}
        onLoadAndInsertAll={(query) =>
          setValue((previous) => withoutDuplicates(previous, matches(query)))
        }
        maxInputLength={500}
        creatable
      />
      <span className="mt-10 text-xs text-gray-400">
        {value.length} selected, {options.length} loaded
        {total !== undefined && ` of ${total}`}
      </span>
    </div>
  );
};

export const ServerSearchWithPaging: Story = { render: () => <ServerSearch /> };

export const WithFileImport: Story = {
  render: () => (
    <Stateful
      label="Regions"
      tooltip="A dropped or pasted file's lines become values, one per line."
      options={REGIONS}
      onResolve={() => {}}
    />
  ),
};

const ManyValues = () => {
  const [value, setValue] = useState<SelectOptionT[]>(
    Array.from({ length: 201 }, (_, i) => ({
      value: `value-${i}`,
      label: `Value ${i}`,
    })),
  );
  return (
    <div className="flex w-96 flex-col">
      <ComboBoxMultiField
        label="Regions"
        options={REGIONS}
        value={value}
        onChange={setValue}
      />
      <span className="mt-10 text-xs text-gray-400">
        above 200 values the field only offers to clear them
      </span>
    </div>
  );
};

export const TooManyValues: Story = { render: () => <ManyValues /> };

export const WithError: Story = {
  render: () => (
    <Stateful
      label="Regions"
      options={REGIONS}
      errorMessage="Pick at least one region"
    />
  ),
};

export const Disabled: Story = {
  render: () => (
    <Stateful
      label="Regions"
      options={REGIONS}
      defaultValue={[REGIONS[0], REGIONS[2]]}
      isDisabled
    />
  ),
};
