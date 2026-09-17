import { tv } from "tailwind-variants";

import type { ColumnDescription } from "../../api/types";
import type { EntityEvent } from "../reducer";

const badge = tv({
  base: [
    "rounded",
    "bg-primary-500",
    "px-1 py-px",
    "text-xs",
    "text-white",
    "font-bold",
  ],
});

interface Props {
  event: EntityEvent;
  className?: string;
  sourceColumn: ColumnDescription;
}

export const RawDataBadge = ({ className, event, sourceColumn }: Props) => {
  return (
    // biome-ignore lint/a11y/noStaticElementInteractions: TODO make this a button
    // biome-ignore lint/a11y/useKeyWithClickEvents: TODO make this a button
    <div
      className={badge({ className })}
      onClick={() => {
        if (navigator.clipboard) {
          navigator.clipboard.writeText(JSON.stringify(event, null, 2));
        }
      }}
    >
      {event[sourceColumn.label] as string}
    </div>
  );
};
