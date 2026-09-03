import type { ReactNode, Ref } from "react";
import { tv } from "tailwind-variants";

import { IndexPrefix } from "../common/components/IndexPrefix";
import { exists } from "../common/helpers/exists";
import InfoTooltip from "../tooltip/InfoTooltip";

import Label from "./Label";

const root = tv({
  variants: {
    fullWidth: { true: "w-full [&_input]:w-full" },
  },
});

interface Props {
  label: ReactNode;
  indexPrefix?: number;
  className?: string;
  tinyLabel?: boolean;
  largeLabel?: boolean;
  fullWidth?: boolean;
  children?: React.ReactNode;
  tooltip?: string;
  htmlFor?: string;
}

const Labeled = ({
  ref,
  indexPrefix,
  className,
  fullWidth,
  label,
  tinyLabel,
  largeLabel,
  tooltip,
  htmlFor,
  children,
}: Props & { ref?: Ref<HTMLLabelElement> }) => {
  return (
    <label
      ref={ref}
      className={root({ fullWidth, className })}
      htmlFor={htmlFor}
    >
      <Label fullWidth={fullWidth} tiny={tinyLabel} large={largeLabel}>
        {exists(indexPrefix) && <IndexPrefix># {indexPrefix}</IndexPrefix>}
        {label}
        {exists(tooltip) && <InfoTooltip text={tooltip} />}
      </Label>
      {children}
    </label>
  );
};

export default Labeled;
