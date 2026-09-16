import type { ReactNode, Ref } from "react";

import { Label, type LabelProps } from "../Label";

/** the select box below its label; downshift's label props connect the two */
export const Labeled = ({
  ref,
  className,
  label,
  children,
  ...labelProps
}: Omit<LabelProps, "children"> & {
  ref?: Ref<HTMLDivElement>;
  className?: string;
  label: string;
  children: ReactNode;
}) => (
  <div ref={ref} className={className}>
    <Label {...labelProps}>{label}</Label>
    {children}
  </div>
);
