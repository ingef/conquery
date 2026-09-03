import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

const grid = tv({
  base: [
    "grow",
    "grid",
    "gap-[3px]",
    "h-full w-full",
    "place-items-center",
    "overflow-auto",
  ],
});

const connector = tv({
  base: [
    "flex items-center justify-center",
    "px-[5px]",
    "rounded",
    "uppercase",
    "text-sm",
    "text-black",
    "select-none",
  ],
});

export const Grid = ({ className, ...props }: ComponentProps<"div">) => (
  <div className={grid({ className })} {...props} />
);

export const Connector = ({ className, ...props }: ComponentProps<"span">) => (
  <span className={connector({ className })} {...props} />
);
