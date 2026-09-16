import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

const indexPrefix = tv({
  base: [
    "inline-block",
    "shrink-0",
    "mr-[7px]",
    "p-[3px]",
    "rounded",
    "bg-primary-50",
    "text-[11px]",
    "leading-none",
  ],
});

export const IndexPrefix = ({
  className,
  ...props
}: ComponentProps<"span">) => (
  <span className={indexPrefix({ className })} {...props} />
);
