// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

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

const SubMessage = styled("p")`
  font-size: ${({ theme }) => theme.font.md};
  margin: 0 0 10px;
`;

const Preview = styled("div")`
  border-radius: 3px;
  background-color: ${({ theme }) => theme.col.grayVeryLight};
  width: 100%;
  height: ${({ large }) => (large ? "100px" : "70px")};
  margin: 5px 0;
`;

export default () => (
  <Root>
    <MsgContainer>
      <Msg>
        <Message>{T.translate("previousQueries.noQueriesFound")}</Message>
        <SubMessage>
          {T.translate("categoryTreeList.noTreesExplanation")}
        </SubMessage>
      </Msg>
    </MsgContainer>
    <Preview large />
    <Preview />
    <Preview large />
  </Root>
);
