import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";

import { SearchField } from "./SearchField";

export default {
  title: "FormComponents/SearchField",
  component: SearchField,
  parameters: { layout: "centered" },
} as Meta<typeof SearchField>;

type Story = StoryObj<typeof SearchField>;

// Enter and the search button submit, Escape and the clear button clear
const Stateful = () => {
  const [value, setValue] = useState("");
  const [submitted, setSubmitted] = useState<string | null>(null);

  return (
    <div className="flex w-72 flex-col gap-2">
      <SearchField
        aria-label="Search regions"
        placeholder="Search regions"
        value={value}
        onChange={setValue}
        onSubmit={setSubmitted}
        onClear={() => setSubmitted(null)}
      />
      <span className="text-xs text-gray-500">
        searched for: {submitted ?? "nothing yet"}
      </span>
    </div>
  );
};

export const Default: Story = {
  render: () => <Stateful />,
};
