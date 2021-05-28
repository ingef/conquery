import { useEffect, useRef } from "react";

type Timeout = ReturnType<typeof setTimeout>;

// Note: No need to add callbacks used inside `fn` to the debounceTriggers
//       because fn will be saved in a ref / will always be up to date
export const useDebounce = (
  fn: (...args: any) => any,
  ms: number,
  debounceTriggers: any[],
) => {
  const handle = useRef<Timeout | null>(null);
  const fnRef = useRef<(...args: any) => any>(fn);
  const msRef = useRef<number>(ms);

  useEffect(() => {
    fnRef.current = fn;
  }, [fn]);
  useEffect(() => {
    msRef.current = ms;
  }, [ms]);

  useEffect(() => {
    if (handle.current) clearTimeout(handle.current);

    handle.current = setTimeout(
      fnRef.current.bind(null, ...debounceTriggers),
      msRef.current,
    );

    return () => {
      if (handle.current) clearTimeout(handle.current);
    };

    // Yes, dependencies can't be statically verified,
    // but this way, useDebounce almost works like useEffect
    // eslint-disable-next-line
  }, debounceTriggers);
};
