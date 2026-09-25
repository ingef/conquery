import type { ReactNode } from "react";
import { ToggleButton } from "react-aria-components";
import { useHotkeys } from "react-hotkeys-hook";
import { tv } from "tailwind-variants";

import { textStyle } from "../ui-components/Typography";

// a chip that toggles: dotted while off, the selected look with a solid frame while on
const badgeToggleButton = tv({
  base: [
    "inline-flex items-center",
    "rounded",
    "px-1 py-px",
    "border-2 border-dotted border-gray-100",
    textStyle({ size: 2, tone: "muted", strong: true }),
    "whitespace-nowrap",
    "cursor-pointer",
    "hover:bg-gray-50",
    "outline-none data-focus-visible:outline-2 data-focus-visible:outline-primary-500",
    "data-selected:border-solid data-selected:border-primary-500",
    "data-selected:bg-primary-50 data-selected:text-primary-500",
    "data-selected:hover:bg-primary-100",
  ],
});

const superScript = tv({
  base: [
    "inline-block",
    "pl-[3px]",
    "translate-x-px -translate-y-[2px]",
    textStyle({ size: 3, tone: "muted" }),
  ],
});

interface Props {
  className?: string;
  selected?: boolean;
  onClick: () => void;
  children: ReactNode;
  hotkey?: string;
}

export const BadgeToggleButton = ({
  className,
  selected,
  onClick,
  children,
  hotkey,
}: Props) => {
  useHotkeys(hotkey || "", onClick, { enabled: !!hotkey }, [hotkey, onClick]);

  return (
    <ToggleButton
      className={badgeToggleButton({ className })}
      isSelected={!!selected}
      onChange={onClick}
    >
      {!selected && "+ "}
      {children}
      {hotkey && <span className={superScript()}>{hotkey}</span>}
    </ToggleButton>
  );
};
