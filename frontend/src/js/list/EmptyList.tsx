import styled from "@emotion/styled";
import React from "react";

const Root = styled("div")`
  position: relative;
  display: flex;
  width: 100%;
  flex-direction: column;
`;

const MsgContainer = styled("div")`
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
`;

const Msg = styled("div")`
  width: 400px;
  white-space: initial;
`;

const Message = styled("p")`
  font-size: ${({ theme }) => theme.font.lg};
  margin: 10px 0 0;
  font-weight: 400;
`;

const Preview = styled("div")<{ large?: boolean }>`
  border-radius: ${({ theme }) => theme.borderRadius};
  background-color: ${({ theme }) => theme.col.grayVeryLight};
  width: 100%;
  height: ${({ large }) => (large ? "100px" : "70px")};
  margin: 5px 0;
`;

interface PropsT {
  emptyMessage: React.ReactNode;
}

const EmptyList: React.FC<PropsT> = ({ emptyMessage }) => (
  <Root>
    <MsgContainer>
      <Msg>
        <Message>{emptyMessage}</Message>
      </Msg>
    </MsgContainer>
    <Preview large />
    <Preview />
    <Preview large />
  </Root>
);

export default EmptyList;
