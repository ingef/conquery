import { ArrowUpIcon } from "lucide-react";
import {
  type HTMLAttributes,
  type PropsWithChildren,
  useEffect,
  useRef,
  useState,
} from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";

const root = tv({ base: "overflow-auto" });

// a white backing, because the secondary button is transparent over the content
const scrollTopButton = tv({
  base: [
    "absolute right-5 bottom-5",
    "z-3",
    "rounded",
    "bg-white",
    "shadow-[1px_1px_5px_0px_rgba(0,0,0,0.2)]",
  ],
});

export default function ScrollBox({
  children,
  className,
  ...props
}: PropsWithChildren<HTMLAttributes<HTMLDivElement>>) {
  const { t } = useTranslation();
  const scrollBoxRef = useRef<HTMLDivElement>(null);
  const [showButton, setShowButton] = useState(false);

  useEffect(() => {
    const scrollHandler = (e: Event) => {
      const target = e.target as HTMLDivElement;
      setShowButton(target.scrollTop > 0);
    };

    const scrollBox = scrollBoxRef.current;
    scrollBox?.addEventListener("scroll", scrollHandler, {
      passive: true,
    });
    return () => scrollBox?.removeEventListener("scroll", scrollHandler);
  }, []);

  return (
    <div ref={scrollBoxRef} className={root({ className })} {...props}>
      {showButton && (
        <div className={scrollTopButton()}>
          <Button
            intent="secondary"
            aria-label={t("preview.scrollToTop")}
            onPress={() =>
              scrollBoxRef.current?.scrollTo({ top: 0, behavior: "smooth" })
            }
          >
            <ArrowUpIcon />
          </Button>
        </div>
      )}
      {children}
    </div>
  );
}
