import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

const heading3 = tv({ base: "text-lg font-normal text-gray-800" });
const heading4 = tv({ base: "text-sm font-normal text-gray-500 uppercase" });

export const Heading3 = ({ className, ...props }: ComponentProps<"h3">) => (
  <h3 className={heading3({ className })} {...props} />
);

export const Heading4 = ({ className, ...props }: ComponentProps<"h4">) => (
  <h4 className={heading4({ className })} {...props} />
);
