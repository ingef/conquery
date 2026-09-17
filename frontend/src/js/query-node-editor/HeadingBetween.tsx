import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

import { Heading4 } from "../headings/Headings";

const headingBetween = tv({ base: ["mx-[15px] mt-[15px]"] });

export const HeadingBetween = ({
  className,
  ...props
}: ComponentProps<typeof Heading4>) => (
  <Heading4 className={headingBetween({ className })} {...props} />
);
