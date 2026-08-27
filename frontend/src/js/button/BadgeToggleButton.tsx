import type { ReactNode } from "react";
import { useHotkeys } from "react-hotkeys-hook";

import { tv } from "../tv";

import BasicButton from "./BasicButton";

const badgeToggleButton = tv({
  base: ["rounded", "px-1 py-px", "text-sm", "font-bold", "whitespace-nowrap"],
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
    "text-tiny",
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
    <BasicButton
      className={badgeToggleButton({ active: !!active, className })}
      onClick={onClick}
    >
      {!active && "+ "}
      {children}
      {hotkey && <span className={superScript()}>{hotkey}</span>}
    </BasicButton>
  );
};
