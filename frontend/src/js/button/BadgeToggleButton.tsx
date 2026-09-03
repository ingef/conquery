import type { ReactNode } from "react";
import { useHotkeys } from "react-hotkeys-hook";

import { tv } from "tailwind-variants";

import { Button } from "../ui-components/Button";

const badgeToggleButton = tv({
  base: ["h-auto px-1 py-px", "text-sm", "font-bold"],
  variants: {
    active: {
      true: [
        "border-2 border-primary-500",
        "bg-white hover:bg-gray-50",
        "text-primary-500",
      ],
      false: [
        "border-2 border-dotted border-gray-100",
        "hover:bg-bg-50",
        "text-gray-500",
      ],
    },
  },
});

const superScript = tv({
  base: [
    "inline-block",
    "pl-[3px]",
    "translate-x-px -translate-y-[2px]",
    "text-[11px]",
    "text-gray-500",
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
    <Button
      intent="tertiary"
      size="sm"
      aria-pressed={!!active}
      className={badgeToggleButton({ active: !!active, className })}
      onPress={onClick}
    >
      {!active && "+ "}
      {children}
      {hotkey && <span className={superScript()}>{hotkey}</span>}
    </Button>
  );
};
