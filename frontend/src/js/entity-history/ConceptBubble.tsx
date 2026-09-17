import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

const conceptBubble = tv({
  base: [
    "px-[3px]",
    "rounded",
    "border border-gray-500",
    "bg-white",
    "text-sm",
    "text-gray-800",
  ],
});

export const ConceptBubble = ({
  className,
  ...props
}: ComponentProps<"span">) => (
  <span className={conceptBubble({ className })} {...props} />
);
