import { ExternalLinkIcon } from "lucide-react";
import type { ReactNode } from "react";
import {
  Link as RacLink,
  type LinkProps as RacLinkProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { textStyle } from "./Typography";

// the look of Button's link intent
const link = tv({
  base: [
    "inline-flex items-center",
    "gap-1",
    textStyle({ size: 2, strong: true }),
    "text-gray-600",
    "cursor-pointer",
    "outline-none",
    "hover:text-gray-800 hover:underline",
    "data-focus-visible:outline-2 data-focus-visible:outline-primary-500",
  ],
});

export interface LinkProps
  extends Omit<RacLinkProps, "className" | "style" | "children"> {
  href: string;
  children: ReactNode;
  /** opens in a new tab and says so with an icon */
  external?: boolean;
}

/** A text link, react-aria's Link underneath; a button that only navigates is Button's link intent */
export const Link = ({ external, children, ...props }: LinkProps) => (
  <RacLink
    className={link()}
    target={external ? "_blank" : undefined}
    rel={external ? "noreferrer" : undefined}
    {...props}
  >
    {children}
    {external && <ExternalLinkIcon />}
  </RacLink>
);
