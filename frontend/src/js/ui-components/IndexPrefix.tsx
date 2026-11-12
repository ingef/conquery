import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

import { textStyle } from "./Typography";

const indexPrefix = tv({
  base: [
    "inline-flex items-center",
    "shrink-0",
    "h-4 px-[3px]",
    "mr-[7px]",
    "rounded",
    "bg-primary-50",
    textStyle({ size: 3 }),
  ],
});

export const IndexPrefix = ({
  className,
  ...props
}: ComponentProps<"span">) => (
  <span className={indexPrefix({ className })} {...props} />
);
