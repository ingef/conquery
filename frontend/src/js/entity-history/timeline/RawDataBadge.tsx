import { tv } from "tailwind-variants";
import type { ColumnDescription } from "../../api/types";
import { textStyle } from "../../ui-components/Typography";
import type { EntityEvent } from "../reducer";

const badge = tv({
  base: [
    "rounded",
    "bg-primary-500",
    "px-1 py-px",
    textStyle({ size: 3, strong: true }),
    "text-white",
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
