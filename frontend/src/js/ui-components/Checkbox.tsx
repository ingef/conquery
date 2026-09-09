import { faCheck } from "@fortawesome/free-solid-svg-icons";
import type { ReactNode } from "react";
import {
  Checkbox as RacCheckbox,
  type CheckboxProps as RacCheckboxProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { exists } from "../common/helpers/exists";
import { Icon } from "./Icon";
import InfoTooltip from "./InfoTooltip";
import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
  tooltipDelay,
} from "./Tooltip";

const root = tv({
  base: [
    "group",
    "inline-flex items-start",
    "gap-1",
    "cursor-pointer select-none",
    "text-sm font-medium",
    "data-disabled:cursor-not-allowed",
  ],
});

// the 30 px hit area of the box, a control's height
const frame = tv({
  base: ["flex items-center justify-center", "size-[30px] shrink-0"],
});

const box = tv({
  base: [
    "flex items-center justify-center",
    "size-[18px]",
    "rounded",
    "border-2 border-primary-500",
    "bg-white text-white",
    "group-data-selected:bg-primary-500",
    "group-data-focus-visible:outline-2 group-data-focus-visible:outline-offset-2 group-data-focus-visible:outline-primary-500",
    "group-data-disabled:opacity-50",
  ],
});

// 5 px above and below a 20 px line: the first line centers on the box,
// further lines flow below it
const label = tv({
  base: ["inline-flex items-center", "gap-[6px]", "py-[5px]", "leading-5"],
});

export interface CheckboxProps
  extends Omit<RacCheckboxProps, "className" | "style" | "children"> {
  children?: ReactNode;
  /** shown on the box after a while, e.g. why it is disabled */
  tooltip?: string;
  /** a help icon after the label */
  infoTooltip?: string;
}

/**
 * A checkbox with its label, react-aria's Checkbox underneath: `isSelected` /
 * `onChange`, `isDisabled`, keyboard and form support. The children are the
 * label, an Icon may lead. Layout around it belongs to the parent.
 *
 *   <Checkbox isSelected={exclude} onChange={setExclude}>
 *     {t("queryNodeEditor.excludeTimestamps")}
 *   </Checkbox>
 */
export const Checkbox = ({
  children,
  tooltip,
  infoTooltip,
  ...props
}: CheckboxProps) => (
  <RacCheckbox className={root()} {...props}>
    {({ isSelected }) => (
      <>
        <TooltipTrigger delay={tooltipDelay.long}>
          <TooltipTarget as="span" excludeFromTabOrder className={frame()}>
            <span className={box()}>
              {isSelected && <Icon icon={faCheck} />}
            </span>
          </TooltipTarget>
          <Tooltip>{tooltip}</Tooltip>
        </TooltipTrigger>
        <span className={label()}>{children}</span>
        {exists(infoTooltip) && <InfoTooltip text={infoTooltip} />}
      </>
    )}
  </RacCheckbox>
);
