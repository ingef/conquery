import { useEffect, useRef } from "react";

type Timeout = ReturnType<typeof setTimeout>;

// Note: No need to add callbacks used inside `fn` to the debounceTriggers
//       because fn will be saved in a ref / will always be up to date
export const useDebounce = (
  fn: (...args: unknown[]) => unknown,
  delay: number,
  debounceTriggers: unknown[],
) => {
  const handle = useRef<Timeout | null>(null);
  const fnRef = useRef<(...args: unknown[]) => unknown>(fn);
  const delayRef = useRef<number>(delay);

  useEffect(() => {
    fnRef.current = fn;
  }, [fn]);
  useEffect(() => {
    delayRef.current = delay;
  }, [delay]);

  // biome-ignore lint/correctness/useExhaustiveDependencies: dependencies are passed in by the caller, so useDebounce works like useEffect
  useEffect(() => {
    if (handle.current) clearTimeout(handle.current);

    handle.current = setTimeout(
      fnRef.current.bind(null, ...debounceTriggers),
      delayRef.current,
    );

    return () => {
      if (handle.current) clearTimeout(handle.current);
    };
  }, debounceTriggers);
};
