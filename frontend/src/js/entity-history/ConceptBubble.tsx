import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

import { textStyle } from "../ui-components/Typography";

const conceptBubble = tv({
  base: [
    "px-[3px]",
    "rounded",
    "border border-gray-500",
    "bg-white",
    textStyle({ size: 2, tone: "default" }),
  ],
});

export const ConceptBubble = ({
  className,
  ...props
}: ComponentProps<"span">) => (
  <span className={conceptBubble({ className })} {...props} />
);
