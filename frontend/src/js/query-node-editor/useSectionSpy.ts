import {
  type RefObject,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";

export const COMMON_SECTION = "common";

// the strip below the container's top edge that a section crosses to become current
const ACTIVATION_STRIP = "0px 0px -75% 0px";

const byDocumentOrder = (a: Element, b: Element) =>
  a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING ? -1 : 1;

/**
 * Which section of a scrolling column is the current one, without scroll
 * events: the deepest section crossing the activation strip, the first
 * section while the start sentinel is in view, the last one while the end
 * sentinel is. Sections register by key, the sentinels mark the content's ends.
 */
export function useSectionSpy(containerRef: RefObject<HTMLElement | null>) {
  const [activeSection, setActiveSection] = useState(COMMON_SECTION);

  const sections = useRef(new Map<string, HTMLElement>());
  const crossing = useRef(new Set<Element>());
  const edges = useRef({ start: true, end: false });
  const sentinels = useRef<{ start: Element | null; end: Element | null }>({
    start: null,
    end: null,
  });
  const observers = useRef<{
    strip: IntersectionObserver;
    edge: IntersectionObserver;
  } | null>(null);

  const decide = useCallback(() => {
    const ordered = [...sections.current.entries()].sort((a, b) =>
      byDocumentOrder(a[1], b[1]),
    );
    if (ordered.length === 0) return;

    const current = edges.current.start
      ? ordered[0]
      : edges.current.end
        ? ordered[ordered.length - 1]
        : ordered.filter(([, element]) => crossing.current.has(element)).at(-1);

    if (current) setActiveSection(current[0]);
  }, []);

  useEffect(() => {
    const root = containerRef.current;
    if (!root) return;

    const strip = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            crossing.current.add(entry.target);
          } else {
            crossing.current.delete(entry.target);
          }
        }
        decide();
      },
      { root, rootMargin: ACTIVATION_STRIP },
    );
    const edge = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.target === sentinels.current.start) {
            edges.current.start = entry.isIntersecting;
          } else if (entry.target === sentinels.current.end) {
            edges.current.end = entry.isIntersecting;
          }
        }
        decide();
      },
      { root, threshold: 1 },
    );

    for (const element of sections.current.values()) strip.observe(element);
    for (const sentinel of Object.values(sentinels.current)) {
      if (sentinel) edge.observe(sentinel);
    }
    observers.current = { strip, edge };

    return () => {
      strip.disconnect();
      edge.disconnect();
      observers.current = null;
    };
  }, [containerRef, decide]);

  const registerSection = useCallback(
    (key: string) => (element: HTMLElement | null) => {
      const previous = sections.current.get(key);
      if (previous) {
        observers.current?.strip.unobserve(previous);
        crossing.current.delete(previous);
      }
      if (element) {
        sections.current.set(key, element);
        observers.current?.strip.observe(element);
      } else {
        sections.current.delete(key);
      }
    },
    [],
  );

  const registerSentinel = useCallback(
    (edgeName: "start" | "end") => (element: HTMLElement | null) => {
      const previous = sentinels.current[edgeName];
      if (previous) observers.current?.edge.unobserve(previous);
      sentinels.current[edgeName] = element;
      if (element) observers.current?.edge.observe(element);
    },
    [],
  );

  const scrollToSection = useCallback((key: string) => {
    sections.current
      .get(key)
      ?.scrollIntoView({ block: "start", behavior: "smooth" });
  }, []);

  return { activeSection, registerSection, registerSentinel, scrollToSection };
}
