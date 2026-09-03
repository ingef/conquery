import type { ReactNode } from "react";
import { tv } from "tailwind-variants";

const root = tv({ base: ["relative", "flex flex-col", "w-full"] });

const msgContainer = tv({
  base: ["flex flex-col items-start justify-center", "h-full w-full"],
});

const message = tv({
  base: ["mx-0 mt-[10px] mb-0", "text-xl", "font-normal"],
});

const preview = tv({
  base: ["my-[5px]", "h-[70px] w-full", "rounded", "bg-gray-50"],
  variants: {
    large: { true: "h-[100px]" },
  },
});

const EmptyList = ({ emptyMessage }: { emptyMessage: ReactNode }) => (
  <div className={root()}>
    <div className={msgContainer()}>
      <div className="whitespace-nowrap">
        <p className={message()}>{emptyMessage}</p>
      </div>
    </div>
    <div className={preview({ large: true })} />
    <div className={preview()} />
    <div className={preview({ large: true })} />
  </div>
);

export default EmptyList;
