import styled from "@emotion/styled";
import { useRef, useCallback } from "react";

import { useIntersectionObserver } from "../../common/useIntersectionObserver";

const Span = styled("span")`
  width: 1px;
  height: 1px;
  background: transparent;
  pointer-events: none;
  display: block;
`;

interface Props {
  onLoadMore: () => void;
}

const LoadMoreSentinel = ({ onLoadMore }: Props) => {
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

  return <Span ref={intersectionObserverRef} />;
};

export default LoadMoreSentinel;
