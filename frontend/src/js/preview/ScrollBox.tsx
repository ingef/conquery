import { faArrowUp } from "@fortawesome/free-solid-svg-icons";
import {
  type HTMLAttributes,
  type PropsWithChildren,
  useEffect,
  useRef,
  useState,
} from "react";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";

const root = tv({ base: "overflow-auto" });

const scrollTopButton = tv({
  base: [
    "absolute right-[30px] bottom-[30px]",
    "z-3",
    "flex justify-center",
    "h-[50px] w-[50px]",
    "rounded-full",
    "border border-gray-500",
    "bg-white",
    "shadow-[0_0_5px_0_rgba(0,0,0,0.2)]",
  ],
});

export default function ScrollBox({
  threshold = 0,
  children,
  className,
  ...props
}: PropsWithChildren<{ threshold?: number }> & HTMLAttributes<HTMLDivElement>) {
  const scrollBoxRef = useRef<HTMLDivElement>(null);
  const [showButton, setShowButton] = useState(false);

  useEffect(() => {
    const scrollHandler = (e: Event) => {
      const target = e.target as HTMLDivElement;
      setShowButton(target.scrollTop > threshold);
    };

    const scrollBox = scrollBoxRef.current;
    scrollBox?.addEventListener("scroll", scrollHandler, {
      passive: true,
    });
    return () => scrollBox?.removeEventListener("scroll", scrollHandler);
  }, [threshold]);

  return (
    <div ref={scrollBoxRef} className={root({ className })} {...props}>
      {showButton && (
        <Button
          intent="tertiary"
          onPress={() =>
            scrollBoxRef.current?.scrollTo({ top: 0, behavior: "smooth" })
          }
          className={scrollTopButton()}
        >
          <Icon icon={faArrowUp} />
        </Button>
      )}
      {children}
    </div>
  );
}
