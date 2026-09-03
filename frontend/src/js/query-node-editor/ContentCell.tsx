import type { ReactNode, Ref } from "react";
import { tv } from "tailwind-variants";

import { Heading4 } from "../headings/Headings";

const root = tv({
  base: ["shrink-0", "flex flex-col", "min-w-[220px]"],
});

const content = tv({
  base: ["grow", "px-[10px] py-[3px]"],
});

const headlineHeading = tv({
  base: ["mx-[10px] mt-[14px]"],
});

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
    {headline && <Heading4 className={headlineHeading()}>{headline}</Heading4>}
    <div className={content()}>{children}</div>
  </div>
);

export default ContentCell;
