import type { ReactNode } from "react";

import { H4 } from "../ui-components/Typography";

/** a heading between the sections of a column */
export const HeadingBetween = ({ children }: { children: ReactNode }) => (
  <div className="mx-[15px] mt-[15px]">
    <H4>{children}</H4>
  </div>
);
