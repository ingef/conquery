import type { ReactNode } from "react";
import { tv } from "tailwind-variants";

const keyShape = tv({
  base: [
    "px-1 py-[2px]",
    "rounded",
    "border border-gray-100",
    "shadow-[0_0_3px_0_var(--color-gray-100)]",
    "text-xs",
    "leading-none",
    "uppercase",
  ],
});

export const KeyboardKey = ({ children }: { children: ReactNode }) => (
  <kbd className={keyShape()}>{children}</kbd>
);
