import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

import { C2 } from "../../ui-components/Typography";

const description = tv({ base: "mx-[10px] mb-[10px] last:mb-0" });

/** a form's explanatory text; `dangerouslySetInnerHTML` for markup from the form config */
export const Description = ({
  className,
  ...props
}: Omit<ComponentProps<typeof C2>, "as" | "tone" | "strong" | "truncate"> & {
  className?: string;
}) => (
  <div className={description({ className })}>
    <C2 {...props} />
  </div>
);
