import type { ReactNode } from "react";
import { tv } from "tailwind-variants";

import { exists } from "../../common/helpers/exists";
import { H3, H4, H5 } from "../../ui-components/Typography";

// the config's h1/h2/h3 are the form's own levels; the app's outline starts at h3 inside the form pane
const HEADING = { h1: H3, h2: H4, h3: H5 } as const;

const spacing = tv({
  base: "first:mt-0",
  variants: {
    size: {
      h1: "mt-5 mb-[5px]",
      h2: "mt-[10px] mb-[3px]",
      h3: "mt-[10px] mb-[3px]",
    },
  },
  defaultVariants: { size: "h1" },
});

export const Headline = ({
  size = "h1",
  index,
  children,
}: {
  size?: "h1" | "h2" | "h3";
  /** the number of a top-level section */
  index?: number;
  children: ReactNode;
}) => {
  const Heading = HEADING[size];
  return (
    <div className={spacing({ size })}>
      <Heading>
        {exists(index) && <span className="mr-2 text-gray-400">{index}</span>}
        {children}
      </Heading>
    </div>
  );
};
