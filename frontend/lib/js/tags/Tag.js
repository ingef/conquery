// @flow

import * as React from "react";
import styled from "@emotion/styled";
import { css } from "@emotion/core";

type PropsType = {
  children?: React.Node,
  className?: string,
  isSelected: boolean,
  onClick: () => void
};

const Root = styled("p")`
  display: inline-block;
  padding: 2px 4px;
  margin: 0 3px 3px 0;
  font-size: ${({ theme }) => theme.font.xs};
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.blueGrayLight};

  ${({ isClickable, theme }) =>
    isClickable &&
    css`
      cursor: pointer;

      &:hover {
        border-color: ${theme.col.blueGrayDark};
      }
    `};

  ${({ isSelected, theme }) =>
    isSelected &&
    css`
      background-color: ${theme.col.grayVeryLight};
      border-color: ${theme.col.blueGrayDark};
    `};
`;

const Tag = (props: PropsType) => {
  return (
    <Root
      className={props.className}
      isClickable={!!props.onClick}
      isSelected={!!props.isSelected}
      onClick={props.onClick}
    >
      {props.children}
    </Root>
  );
};

export default Tag;
