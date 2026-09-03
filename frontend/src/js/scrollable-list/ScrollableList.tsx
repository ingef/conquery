import type { ReactNode } from "react";
import { tv } from "tailwind-variants";
import { IncrementalList } from "../common/components/IncrementalList";

interface PropsType {
  items: ReactNode[];
  maxVisibleItems: number;
  fullWidth?: boolean;
  dataTestId?: string;
}

const root = tv({
  base: [
    "overflow-y-auto",
    "[-webkit-overflow-scrolling:touch]",
    "max-w-[340px]",
    "rounded-[2px]",
    "border border-gray-400",
    "text-gray-800",
  ],
  variants: {
    fullWidth: { true: "w-full max-w-full" },
  },
});

const item = tv({
  base: [
    "max-w-full",
    "px-[10px]",
    "border-b border-gray-50",
    "overflow-hidden text-ellipsis whitespace-nowrap",
    "text-sm",
    "leading-6",
  ],
});

const ScrollableList = ({
  items,
  maxVisibleItems,
  fullWidth,
  dataTestId,
}: PropsType) => {
  const renderItem = (index: number) => {
    return (
      <div key={index} className={item({ className: "scrollable-list-item" })}>
        {items[index]}
      </div>
    );
  };

  return (
    <div
      className={root({ fullWidth: !!fullWidth })}
      // With the number of visible items specified here,
      // make an additional element half-visible at the end to indicate
      // that the list is scrollable
      style={{ maxHeight: (maxVisibleItems + 0.5) * 34 }}
      data-test-id={dataTestId}
    >
      <IncrementalList
        renderItem={renderItem}
        length={items ? items.length : 0}
      />
    </div>
  );
};

export default ScrollableList;
