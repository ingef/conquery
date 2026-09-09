import { faChevronRight } from "@fortawesome/free-solid-svg-icons";
import type { ReactNode } from "react";
import {
  Heading,
  Button as RacButton,
  Disclosure as RacDisclosure,
  DisclosureGroup as RacDisclosureGroup,
  type DisclosureGroupProps as RacDisclosureGroupProps,
  DisclosurePanel as RacDisclosurePanel,
  type DisclosureProps as RacDisclosureProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { Icon } from "./Icon";
import InfoTooltip from "./InfoTooltip";

const root = tv({
  base: ["group", "overflow-hidden", "rounded-sm", "border border-gray-400"],
});

const title = tv({ base: ["flex items-center", "bg-white", "pr-2"] });

const heading = tv({ base: ["grow", "min-w-0", "text-sm font-normal"] });

const trigger = tv({
  base: [
    "flex items-center",
    "gap-3",
    "w-full",
    "py-3 pl-3",
    "text-left",
    "cursor-pointer",
    "outline-none",
    "data-focus-visible:outline-2 data-focus-visible:-outline-offset-2 data-focus-visible:outline-primary-500",
  ],
});

const chevron = tv({
  base: ["transition-transform duration-100", "group-data-expanded:rotate-90"],
});

const panel = tv({ base: ["border-t border-gray-300", "bg-bg-50", "p-3"] });

const group = tv({ base: ["flex flex-col", "gap-2"] });

export interface DisclosureProps
  extends Omit<RacDisclosureProps, "className" | "style" | "children"> {
  children: ReactNode;
}

/**
 * A collapsible section on react-aria-components: `isExpanded` /
 * `onExpandedChange` or `defaultExpanded`; inside a DisclosureGroup it is
 * keyed by `id` and the group holds the state.
 *
 *   <Disclosure defaultExpanded>
 *     <DisclosureTitle info="…" actions={<Button …/>}>Section</DisclosureTitle>
 *     <DisclosurePanel>…</DisclosurePanel>
 *   </Disclosure>
 */
export const Disclosure = ({ children, ...props }: DisclosureProps) => (
  <RacDisclosure className={root()} {...props}>
    {children}
  </RacDisclosure>
);

/**
 * The heading with the trigger button. `info` puts a help icon after it,
 * `actions` further buttons; both sit outside the trigger, which may not
 * contain interactive elements.
 */
export const DisclosureTitle = ({
  children,
  info,
  actions,
}: {
  children: ReactNode;
  info?: string;
  actions?: ReactNode;
}) => (
  <div className={title()}>
    <Heading className={heading()}>
      <RacButton slot="trigger" className={trigger()}>
        <Icon icon={faChevronRight} className={chevron()} />
        {children}
      </RacButton>
    </Heading>
    {info && <InfoTooltip text={info} />}
    {actions}
  </div>
);

export const DisclosurePanel = ({ children }: { children: ReactNode }) => (
  <RacDisclosurePanel className={panel()}>{children}</RacDisclosurePanel>
);

export interface DisclosureGroupProps
  extends Omit<RacDisclosureGroupProps, "className" | "style" | "children"> {
  children: ReactNode;
}

/**
 * Stacks Disclosures and owns their state: `expandedKeys` /
 * `onExpandedChange`, `allowsMultipleExpanded` for more than one open at a time.
 */
export const DisclosureGroup = ({
  children,
  ...props
}: DisclosureGroupProps) => (
  <RacDisclosureGroup className={group()} {...props}>
    {children}
  </RacDisclosureGroup>
);
