import { useTheme, Theme, css } from "@emotion/react";
import styled from "@emotion/styled";
import { FC } from "react";

const Button = styled("button")<{
  selected?: boolean;
  size?: "M" | "L";
}>`
  border: 0;
  background-color: transparent;
  margin: 0 2px;
  padding: ${({ size }) => (size === "L" ? "5px 8px" : "5px 4px")};
  font-size: ${({ theme, size }) =>
    size === "L" ? theme.font.sm : theme.font.xs};

  ${({ size }) =>
    size === "M" &&
    css`
      text-transform: uppercase;
    `};

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
  size: "M" | "L";
  isSelected?: boolean;
  onClick: () => void;
}

const valueToColor = (theme: Theme, value: string) => {
  switch (value) {
    case "own":
      return theme.col.blueGrayDark;
    case "system":
      return theme.col.grayLight;
    default:
      return theme.col.black;
  }
};

const SmallTabNavigationButton: FC<PropsT> = ({
  value,
  children,
  size,
  isSelected,
  onClick,
}) => {
  const theme = useTheme();
  const borderColor = valueToColor(theme, value);

  return (
    <Button
      style={{ borderColor }}
      type="button"
      size={size}
      selected={isSelected}
      onClick={onClick}
    >
      {children}
    </Button>
  );
};

export default SmallTabNavigationButton;
