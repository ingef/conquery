import { keyframes } from "@emotion/react";
import styled from "@emotion/styled";

const blink = keyframes`
  50% {
    color: transparent;
  }
`;

const Dot = styled.span`
  animation: 1s ${blink} infinite;
  &:nth-of-type(1) {
    animation-delay: 0ms;
  }
  &:nth-of-type(2) {
    animation-delay: 250ms;
  }
  &:nth-of-type(3) {
    animation-delay: 500ms;
  }
`;

const Root = styled("span")`
  font-weight: 700;
`;

export default function AnimatedDots() {
  return (
    <Root>
      <Dot>.</Dot>
      <Dot>.</Dot>
      <Dot>.</Dot>
    </Root>
  );
}
