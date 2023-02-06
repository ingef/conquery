import { useEffect } from "react";
import ResizeObserver from "resize-observer-polyfill";

export function useResizeObserver(
  callback: (entry: ResizeObserverEntry) => void,
  element: Element | null,
) {
  useEffect(() => {
    if (!element) return;

    const observer = new ResizeObserver((entries: ResizeObserverEntry[]) => {
      const observedElement = entries[0];

      callback(observedElement);
    });

    observer.observe(element);

    return () => observer.unobserve(element);
  }, [callback, element]);
}
