import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

const tinyLabel = tv({
  base: [
    "mt-[5px]",
    "text-[11px]",
    "font-normal",
    "text-gray-500",
    "uppercase",
    "whitespace-nowrap",
    "leading-none",
  ],
});

export const TinyLabel = ({ className, ...props }: ComponentProps<"p">) => (
  <p className={tinyLabel({ className })} {...props} />
);
