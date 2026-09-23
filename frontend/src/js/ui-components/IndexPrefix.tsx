import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

import { textStyle } from "./Typography";

const indexPrefix = tv({
  base: [
    "inline-block",
    "shrink-0",
    "mr-[7px]",
    "p-[3px]",
    "rounded",
    "bg-primary-50",
    textStyle({ size: 3 }),
    "leading-none",
  ],
});

export const IndexPrefix = ({
  className,
  ...props
}: ComponentProps<"span">) => (
  <span className={indexPrefix({ className })} {...props} />
);
