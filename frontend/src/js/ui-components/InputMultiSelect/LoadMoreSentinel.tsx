import { useCallback, useRef } from "react";
import { tv } from "tailwind-variants";

import { useIntersectionObserver } from "../../common/useIntersectionObserver";

const sentinel = tv({
  base: ["block", "w-px h-px", "bg-transparent", "pointer-events-none"],
});

interface Props {
  className?: string;
  onLoadMore: () => void;
}

const LoadMoreSentinel = ({ onLoadMore, className }: Props) => {
  const intersectionObserverRef = useRef<HTMLSpanElement | null>(null);

  useIntersectionObserver(
    intersectionObserverRef,
    useCallback(
      (_, isIntersecting) => {
        if (isIntersecting) {
          onLoadMore();
        }
      },
      [onLoadMore],
    ),
  );

  return (
    <span className={sentinel({ className })} ref={intersectionObserverRef} />
  );
};

export default LoadMoreSentinel;
