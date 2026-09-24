import type { ReactNode } from "react";

import { C3 } from "../../ui-components/Typography";

/** the label above a value in the timeline's dense grids */
export const TinyLabel = ({ children }: { children: ReactNode }) => (
  <div className="mt-[5px] whitespace-nowrap">
    <C3 tone="muted">{children}</C3>
  </div>
);
