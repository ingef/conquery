import {
  type RefObject,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";

export const COMMON_SECTION = "common";

/**
 * Which section of a scrolling column is the current one: the first section
 * at the start of the scroll, the last one at its end, in between the last
 * section whose top has passed the upper quarter of the viewport. Sections
 * register by key.
 */
export function useSectionSpy(containerRef: RefObject<HTMLElement | null>) {
  const sections = useRef(new Map<string, HTMLElement>());
  const [activeSection, setActiveSection] = useState(COMMON_SECTION);

  const registerSection = useCallback(
    (key: string) => (element: HTMLElement | null) => {
      if (element) {
        sections.current.set(key, element);
      } else {
        sections.current.delete(key);
      }
    },
    [],
  );

  const update = useCallback(() => {
    const container = containerRef.current;
    if (!container || sections.current.size === 0) return;

    const { top, height } = container.getBoundingClientRect();
    const atStart = container.scrollTop <= 1;
    const atEnd =
      container.scrollTop + container.clientHeight >=
      container.scrollHeight - 1;

    const inOrder = [...sections.current]
      .map(([key, element]) => ({
        key,
        offset: element.getBoundingClientRect().top - top,
      }))
      .sort((a, b) => a.offset - b.offset);

    const passed = inOrder.filter(({ offset }) => offset <= height / 4);
    const current = atStart
      ? inOrder[0]
      : atEnd
        ? inOrder[inOrder.length - 1]
        : (passed[passed.length - 1] ?? inOrder[0]);

    setActiveSection(current.key);
  }, [containerRef]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    update();
    container.addEventListener("scroll", update, { passive: true });
    return () => container.removeEventListener("scroll", update);
  }, [containerRef, update]);

  const scrollToSection = useCallback((key: string) => {
    sections.current
      .get(key)
      ?.scrollIntoView({ block: "start", behavior: "smooth" });
  }, []);

  return { activeSection, registerSection, scrollToSection, update };
}
