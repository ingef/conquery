import styled from "@emotion/styled";
import { faArrowUp } from "@fortawesome/free-solid-svg-icons";
import {
  HTMLAttributes,
  PropsWithChildren,
  useEffect,
  useRef,
  useState,
} from "react";
import IconButton from "../button/IconButton";

const Root = styled("div")`
  overflow: auto;
`;
const ScrollTopButton = styled(IconButton)`
  position: absolute;
  right: 30px;
  bottom: 30px;
  width: 50px;
  height: 50px;
  display: flex;
  justify-content: center;
  border-radius: 50%;
  border: 1px solid ${({ theme }) => theme.col.gray};
  background: white;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.2);
`;

export default function ScrollBox({
  threshold = 0,
  children,
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
  }, [scrollBoxRef, threshold]);

  return (
    <Root ref={scrollBoxRef} {...props}>
      {showButton && (
        <ScrollTopButton
          icon={faArrowUp}
          bgHover={true}
          onClick={() =>
            scrollBoxRef.current?.scrollTo({ top: 0, behavior: "smooth" })
          }
        />
      )}
      {children}
    </Root>
  );
}
