// @flow

import React from "react";
import styled from "@emotion/styled";

import BasicButton from "./BasicButton";

const TransparentHeaderButton = styled(BasicButton)`
  color: ${({ theme }) => theme.col.black};
  background-color: transparent;
  border: none;
  border-bottom: ${({ withContent, theme }) =>
    withContent ? theme.col.grayLight : "initial"};

  &:hover {
    background-color: ${({ theme }) => theme.col.grayVeryLight};
  }
`;

export default props => <TransparentHeaderButton {...props} />;
