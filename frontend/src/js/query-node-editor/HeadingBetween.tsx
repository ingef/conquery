import type { ReactNode } from "react";

import { H4 } from "../ui-components/Typography";

/** a muted heading between the sections of a column */
export const HeadingBetween = ({ children }: { children: ReactNode }) => (
  <div className="mx-[15px] mt-[15px]">
    <H4 tone="muted">{children}</H4>
  </div>
);
