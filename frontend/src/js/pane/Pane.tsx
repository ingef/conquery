import * as React from "react";
import styled from "@emotion/styled";
import PaneTabNavigation from "./PaneTabNavigation";

type PropsType = {
  right?: boolean;
  left?: boolean;
  children?: React.Node;
};

const Root = styled("div")`
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

const Pane = (props: PropsType) => {
  const paneType = props.left ? "left" : "right";

  return (
    <Root left={props.left} right={props.right}>
      <Container>
        <PaneTabNavigation paneType={paneType} />
        <Container className="pane__body">{props.children}</Container>
      </Container>
    </Root>
  );
};

export default Pane;
