import type { ReactNode } from "react";
import { tv } from "tailwind-variants";

import { textStyle } from "./Typography";

const keyShape = tv({
  base: [
    "inline-flex items-center",
    "h-4 px-1",
    "rounded",
    "border border-gray-100",
    "shadow-[0_0_3px_0_var(--color-gray-100)]",
    textStyle({ size: 3 }),
    "uppercase",
  ],
});

export const KeyboardKey = ({ children }: { children: ReactNode }) => (
  <kbd className={keyShape()}>{children}</kbd>
);
