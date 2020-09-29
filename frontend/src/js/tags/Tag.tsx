import * as React from "react";
import styled from "@emotion/styled";
import { css } from "@emotion/react";

type PropsType = {
  children?: React.Node;
  className?: string;
  isSelected: boolean;
  onClick: () => void;
};

const Root = styled("p")`
  display: inline-block;
  padding: 4px 4px;
  margin: 0 3px 3px 0;
  font-size: ${({ theme }) => theme.font.xs};
  line-height: ${({ theme }) => theme.font.xs};
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayMediumLight};

  ${({ isClickable, theme }) =>
    isClickable &&
    css`
      cursor: pointer;

      &:hover {
        border-color: ${theme.col.gray};
      }
    `};

  ${({ isSelected, theme }) =>
    isSelected &&
    css`
      background-color: ${theme.col.blueGrayLight};
      color: white;
      font-weight: 700;
      border-color: ${theme.col.blueGrayLight};
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
