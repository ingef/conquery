import {
  useCallback,
  useLayoutEffect,
  useRef,
  RefObject,
  useEffect,
} from "react";

const INTERSECTION_THRESHOLD = 0.5;

export function useIntersectionObserver<T extends Element>(
  domNodeRef: RefObject<T | null>,
  onChange: (domNode: T | null, isIntersecting: boolean) => void,
) {
  const intersecting = useRef(false);

  const onChangeRef = useRef(onChange);
  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  const observer = useRef<IntersectionObserver | null>(
    new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          const isIntersecting =
            entry.isIntersecting &&
            entry.intersectionRatio > INTERSECTION_THRESHOLD;

          if (intersecting.current !== isIntersecting) {
            intersecting.current = isIntersecting;
            onChangeRef.current(domNodeRef.current, isIntersecting);
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

    // Only works if currentDomNodeRef already exists when this effect is being called
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
