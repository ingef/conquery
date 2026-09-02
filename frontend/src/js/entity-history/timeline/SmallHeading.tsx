import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

import { Heading4 } from "../../headings/Headings";

const smallHeading = tv({
  base: ["shrink-0", "text-gray-800", "text-base"],
});

export const SmallHeading = ({
  className,
  ...props
}: ComponentProps<typeof Heading4>) => (
  <Heading4 className={smallHeading({ className })} {...props} />
);
