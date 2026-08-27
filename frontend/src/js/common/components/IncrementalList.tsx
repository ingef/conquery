import { type ReactNode, useEffect, useState } from "react";

import LoadMoreSentinel from "../../ui-components/InputMultiSelect/LoadMoreSentinel";

// Renders the first `pageSize` items and appends another page whenever the
// sentinel at the end scrolls into view. Rows stay mounted once rendered,
// which is fine for the list sizes we have (up to a few thousand rows).
export const IncrementalList = ({
  length,
  pageSize = 100,
  renderItem,
}: {
  length: number;
  pageSize?: number;
  renderItem: (index: number) => ReactNode;
}) => {
  const [visibleCount, setVisibleCount] = useState(pageSize);

  // biome-ignore lint/correctness/useExhaustiveDependencies: a different list length means a different list, start from the first page again
  useEffect(
    function resetOnNewList() {
      setVisibleCount(pageSize);
    },
    [length, pageSize],
  );

  const count = Math.min(visibleCount, length);

  return (
    <>
      {Array.from({ length: count }, (_, index) => renderItem(index))}
      {count < length && (
        <LoadMoreSentinel
          onLoadMore={() => setVisibleCount((c) => c + pageSize)}
        />
      )}
    </>
  );
};
