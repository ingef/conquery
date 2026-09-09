import { faCheck } from "@fortawesome/free-solid-svg-icons";
import {
  CheckboxButton,
  CheckboxField,
  type CheckboxFieldProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { exists } from "../common/helpers/exists";
import { Icon } from "./Icon";
import InfoTooltip from "./InfoTooltip";

// the clickable label. Block-level: as an inline box it would sit on the
// parent's line box, whose height then depends on the parent's font and on
// the baseline of the box
const button = tv({
  base: [
    "group",
    "flex items-start",
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
const label = tv({ base: ["py-[5px]", "leading-5"] });

export interface CheckboxProps
  extends Omit<CheckboxFieldProps, "className" | "style" | "children"> {
  /** the label */
  children: string;
  /** a help icon after the label, e.g. what selecting does or why it is disabled */
  infoTooltip?: string;
}

/**
 * A checkbox with its label, react-aria's CheckboxField + CheckboxButton
 * underneath: `isSelected` / `onChange`, `isDisabled`, keyboard and form
 * support. The children are the label text. Layout around it belongs to the
 * parent.
 *
 *   <Checkbox isSelected={exclude} onChange={setExclude}>
 *     {t("queryNodeEditor.excludeTimestamps")}
 *   </Checkbox>
 */
export const Checkbox = ({
  children,
  infoTooltip,
  ...props
}: CheckboxProps) => (
  <CheckboxField {...props}>
    <CheckboxButton className={button()}>
      {({ isSelected }) => (
        <>
          <span className={frame()}>
            <span className={box()}>
              {isSelected && <Icon icon={faCheck} />}
            </span>
          </span>
          <span className={label()}>
            {children}
            {exists(infoTooltip) && <InfoTooltip text={infoTooltip} />}
          </span>
        </>
      )}
    </CheckboxButton>
  </CheckboxField>
);
