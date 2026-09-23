import type { ReactNode, Ref } from "react";
import { tv } from "tailwind-variants";

import { H4 } from "../ui-components/Typography";

const root = tv({
  base: ["shrink-0", "flex flex-col", "min-w-[220px]"],
});

const content = tv({
  base: ["grow", "px-[10px] py-[3px]"],
});

const headlineHeading = tv({ base: "mx-[10px] mt-[14px]" });

interface PropsT {
  className?: string;
  children?: ReactNode;
  headline?: string;
}

const ContentCell = ({
  ref,
  className,
  headline,
  children,
}: PropsT & { ref?: Ref<HTMLDivElement> }) => (
  <div ref={ref} className={root({ className })}>
    {headline && (
      <div className={headlineHeading()}>
        <H4>{headline}</H4>
      </div>
    )}
    <div className={content()}>{children}</div>
  </div>
);

export default ContentCell;
