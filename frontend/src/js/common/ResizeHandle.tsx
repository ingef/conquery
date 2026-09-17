import type { CSSProperties } from "react";
import { Separator } from "react-resizable-panels";
import { tv } from "tailwind-variants";

const separator = tv({ base: ["relative", "w-px", "bg-gray-400"] });

const handle = tv({
  base: [
    "absolute top-0 -left-[4px]",
    "z-2",
    "h-full w-[9px]",
    "pl-[4px]",
    "cursor-col-resize",
    "bg-gray-50",
    "opacity-0 hover:opacity-100",
    "transition-opacity duration-200 ease-in-out",
  ],
});

const line = tv({ base: ["h-full w-px", "bg-gray-400"] });

export const ResizeHandle = ({
  style,
  disabled,
}: {
  style?: CSSProperties;
  disabled?: boolean;
}) => {
  return (
    <Separator className={separator()} style={style} disabled={disabled}>
      <div className={handle()}>
        <div className={line()} />
      </div>
    </Separator>
  );
};
