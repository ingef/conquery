import React, { FC } from "react";
import styled from "@emotion/styled";
import PaneTabNavigation from "./PaneTabNavigation";

interface PropsT {
  right?: boolean;
  left?: boolean;
}

const Root = styled("div")<{ left?: boolean; right?: boolean }>`
  width: 100%;
  height: 100%;

  padding: ${({ left, right }) => (left || right ? "50px 0 10px" : "0")};
`;

const Container = styled("div")`
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
`;

const Pane: FC<PropsT> = ({ left, right, children }) => {
  const paneType = left ? "left" : "right";

  return (
    <Root left={left} right={right}>
      <Container>
        <PaneTabNavigation paneType={paneType} />
        <Container>{children}</Container>
      </Container>
    </Root>
  );
};

export default Pane;
