import { useCallback, useLayoutEffect, useRef, RefObject } from "react";

const INTERSECTION_THRESHOLD = 0.5;

export function useIntersectionObserver<T extends Element>(
  domNodeRef: RefObject<T | null>,
  onChange: (domNode: T | null, intersecting: boolean) => void,
) {
  const intersecting = useRef(false);
  const observer = useRef<IntersectionObserver | null>(
    new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          const isIntersecting =
            entry.isIntersecting &&
            entry.intersectionRatio > INTERSECTION_THRESHOLD;

          if (intersecting.current !== isIntersecting) {
            intersecting.current = isIntersecting;
            onChange(domNodeRef.current, isIntersecting);
          }
        });
      },
      { threshold: [INTERSECTION_THRESHOLD] },
    ),
  );

  const unobserve = useCallback(() => {
    if (domNodeRef.current && observer.current) {
      observer.current.unobserve(domNodeRef.current);
      observer.current = null;
    }
  }, [domNodeRef]);

  useLayoutEffect(() => {
    const currentDomNodeRef = domNodeRef.current;

    if (currentDomNodeRef && observer.current) {
      observer.current.observe(currentDomNodeRef);
    }

    return () => {
      if (currentDomNodeRef && observer.current) {
        observer.current.unobserve(currentDomNodeRef);
      }
    };
  }, [domNodeRef]);

  return { unobserve };
}
