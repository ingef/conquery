import type { Meta, StoryObj } from "@storybook/react";
import { useState } from "react";

import EditableText from "./EditableText";

export default {
  title: "FormComponents/EditableText",
  component: EditableText,
  parameters: { layout: "centered" },
} as Meta<typeof EditableText>;

type Story = StoryObj<typeof EditableText>;

const Stateful = ({ size }: { size: "sm" | "md" }) => {
  const [text, setText] = useState("Quarterly revenue by region");
  const [editing, setEditing] = useState(false);

  return (
    <div className="w-80">
      <EditableText
        size={size}
        text={text}
        tooltip="Rename"
        editing={editing}
        onSubmit={setText}
        onToggleEdit={() => setEditing(!editing)}
      />
    </div>
  );
};

// as in the query list's name row
export const Small: Story = { render: () => <Stateful size="sm" /> };

// as in the node editor's header
export const Medium: Story = { render: () => <Stateful size="md" /> };
