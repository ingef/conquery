import type { ReactNode } from "react";
import {
  Label as RacLabel,
  type LabelProps as RacLabelProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { exists } from "../common/helpers/exists";
import { IndexPrefix } from "./IndexPrefix";
import InfoTooltip from "./InfoTooltip";

const label = tv({
  base: ["flex items-center", "mt-[6px] mb-[3px]", "font-normal text-gray-800"],
  variants: {
    size: {
      sm: "text-xs",
      md: "text-sm",
    },
    isDisabled: { true: "text-gray-500" },
  },
  defaultVariants: { size: "md" },
});

export interface LabelProps
  extends Omit<RacLabelProps, "className" | "style" | "children"> {
  children: ReactNode;
  /** the position of the field in a numbered list */
  indexPrefix?: number;
  /** a help icon after the text */
  tooltip?: string;
  /** `sm` for a sub-label inside a labeled group */
  size?: "sm" | "md";
  isDisabled?: boolean;
}

/**
 * A field's label. Inside a field, react-aria connects it to the input; on
 * its own it names what follows, like a dropzone.
 */
export const Label = ({
  children,
  indexPrefix,
  tooltip,
  size,
  isDisabled,
  ...props
}: LabelProps) => (
  <RacLabel className={label({ size, isDisabled })} {...props}>
    {exists(indexPrefix) && <IndexPrefix># {indexPrefix}</IndexPrefix>}
    {children}
    {exists(tooltip) && <InfoTooltip text={tooltip} excludeFromTabOrder />}
  </RacLabel>
);

/** a field is named by a visible label or by an aria-label */
export type FieldLabelProps =
  | { label: string; "aria-label"?: never }
  | { label?: never; "aria-label": string };
