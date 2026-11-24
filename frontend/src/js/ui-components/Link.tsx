import { ExternalLinkIcon } from "lucide-react";
import type { ReactNode } from "react";
import {
  Link as RacLink,
  type LinkProps as RacLinkProps,
} from "react-aria-components";

import { type ButtonLook, buttonStyle, isIconOnly } from "./Button";

export type LinkProps = Omit<
  RacLinkProps,
  "className" | "style" | "children"
> & {
  href: string;
  children: ReactNode;
  size?: "sm" | "md" | "lg";
  /** opens in a new tab and says so with an icon */
  external?: boolean;
} & (
    | { intent?: "link" | "primary"; danger?: never }
    | Extract<ButtonLook, { danger?: boolean }>
  );

/**
 * A link in Button's looks, react-aria's Link underneath. It reads as a text
 * link by default; any button intent makes it look like that button, so a
 * navigation that should stand out can be a primary Link.
 *
 *   <Link href={manualUrl} external>Open the manual</Link>
 *   <Link href="/queries" intent="primary">All queries</Link>
 */
export const Link = ({
  intent = "link",
  size,
  danger,
  external,
  children,
  ...props
}: LinkProps) => (
  <RacLink
    className={buttonStyle({
      intent,
      size,
      danger,
      iconOnly: !external && isIconOnly(children),
    })}
    target={external ? "_blank" : undefined}
    rel={external ? "noreferrer" : undefined}
    {...props}
  >
    {children}
    {external && <ExternalLinkIcon />}
  </RacLink>
);
