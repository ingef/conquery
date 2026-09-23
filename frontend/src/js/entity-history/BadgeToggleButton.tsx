import type { ReactNode } from "react";
import { ToggleButton } from "react-aria-components";
import { useHotkeys } from "react-hotkeys-hook";
import { tv } from "tailwind-variants";

import { textStyle } from "../ui-components/Typography";

// a chip that toggles: dotted while off, solid in the primary color while on
const badgeToggleButton = tv({
  base: [
    "inline-flex items-center",
    "rounded",
    "px-1 py-px",
    "border-2 border-dotted border-gray-100",
    textStyle({ size: 2, tone: "muted", strong: true }),
    "whitespace-nowrap",
    "cursor-pointer",
    "hover:bg-bg-50",
    "data-selected:border-solid data-selected:border-primary-500",
    "data-selected:bg-white data-selected:hover:bg-gray-50",
    "data-selected:text-primary-500",
  ],
});

const superScript = tv({
  base: [
    "inline-block",
    "pl-[3px]",
    "translate-x-px -translate-y-[2px]",
    "text-xs",
    "text-gray-600",
  ],
});

interface Props {
  className?: string;
  active?: boolean;
  onClick: () => void;
  children: ReactNode;
  hotkey?: string;
}

export const BadgeToggleButton = ({
  className,
  active,
  onClick,
  children,
  hotkey,
}: Props) => {
  useHotkeys(hotkey || "", onClick, { enabled: !!hotkey }, [hotkey, onClick]);

  return (
    <ToggleButton
      className={badgeToggleButton({ className })}
      isSelected={!!active}
      onChange={onClick}
    >
      {!active && "+ "}
      {children}
      {hotkey && <span className={superScript()}>{hotkey}</span>}
    </ToggleButton>
  );
};
