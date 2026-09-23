import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

import { textStyle } from "../../ui-components/Typography";

// the label above a value in the timeline's dense grids
const tinyLabel = tv({
  base: [
    "mt-[5px]",
    textStyle({ size: 3, tone: "muted" }),
    "leading-none",
    "whitespace-nowrap",
  ],
});

export const TinyLabel = ({ className, ...props }: ComponentProps<"p">) => (
  <p className={tinyLabel({ className })} {...props} />
);
