import React, { FC } from "react";
import styled from "@emotion/styled";

import { useTheme, Theme, css } from "@emotion/react";

const Button = styled("button")<{
  selected?: boolean;
}>`
  border: 0;
  background-color: transparent;
  padding: 5px 4px;
  margin: 0 2px;
  font-size: ${({ theme }) => theme.font.xs};
  text-transform: uppercase;

  ${({ theme, selected }) =>
    selected &&
    css`
      border-bottom: 3px solid ${theme.col.blueGrayLight};
    `};
  ${({ theme, selected }) =>
    !selected &&
    css`
      color: ${theme.col.gray};
      &:hover {
        border-bottom: 3px solid ${theme.col.grayLight} !important;
      }
    `};
`;

interface PropsT {
  value: string;
  isSelected?: boolean;
  onClick: () => void;
}

const valueToColor = (theme: Theme, value: string) => {
  switch (value) {
    case "own":
      return theme.col.orange;
    case "system":
      return theme.col.blueGrayDark;
    default:
      return theme.col.black;
  }
};

const SmallTabNavigationButton: FC<PropsT> = ({
  value,
  children,
  isSelected,
  onClick,
}) => {
  const theme = useTheme();
  const borderColor = valueToColor(theme, value);

  return (
    <Button style={{ borderColor }} selected={isSelected} onClick={onClick}>
      {children}
    </Button>
  );
};

export default SmallTabNavigationButton;
