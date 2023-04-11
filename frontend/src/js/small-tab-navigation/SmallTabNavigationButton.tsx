import { useTheme, Theme, css } from "@emotion/react";
import styled from "@emotion/styled";
import { forwardRef } from "react";

import { HoverNavigatable } from "./HoverNavigatable";

const bottomBorderBase = css`
  content: "";
  position: absolute;
  left: 0px;
  right: 0px;
  height: 3px;
  bottom: 0px;
`;

const Button = styled("button")<{
  selected?: boolean;
  highlightColor: string;
  size?: "M" | "L";
  primary?: boolean;
}>`
  position: relative;

  border-top-left-radius: ${({ theme }) => theme.borderRadius};
  border-top-right-radius: ${({ theme }) => theme.borderRadius};

  transition: border 0.1s ease-in-out;

  border: ${({ primary, theme, selected }) =>
    primary && selected
      ? `1px solid ${theme.col.gray}`
      : primary
      ? "1px solid transparent"
      : "none"};
  &:hover {
    border: ${({ primary, theme, selected }) =>
      primary && selected
        ? `1px solid ${theme.col.gray}`
        : primary
        ? `1px solid ${theme.col.grayMediumLight}`
        : "none"};
    border-bottom: none;
  }
  border-bottom: none;

  background-color: ${({ primary, theme, selected }) =>
    selected && primary ? theme.col.bg : "transparent"};
  margin: 0 2px;
  height: ${({ size }) => (size === "L" ? "30px" : "26px")};
  padding: ${({ size }) => (size === "L" ? "0px 10px" : "0px 3px")};
  font-size: ${({ theme, size }) =>
    size === "L" ? theme.font.sm : theme.font.xs};

  transform: ${({ primary }) => (primary ? "translateY(1px)" : "none")};

  ${({ size }) =>
    size === "M" &&
    css`
      text-transform: uppercase;
    `};

  ${({ selected, primary, highlightColor }) =>
    selected &&
    !primary &&
    css`
      &::after {
        ${bottomBorderBase};
        background-color: ${highlightColor};
      }
    `}
  ${({ theme, selected, primary }) =>
    !selected &&
    !primary &&
    css`
      color: ${theme.col.gray};
      &:hover {
        &::after {
          ${bottomBorderBase};
          background-color: ${theme.col.grayLight};
        }
      }
    `};
`;

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

const SmallTabNavigationButton = forwardRef<
  HTMLButtonElement,
  {
    value: string;
    size: "M" | "L";
    isSelected?: boolean;
    onClick: () => void;
    children?: React.ReactNode;
    variant: "primary" | "secondary";
  }
>(({ value, children, size, isSelected, onClick, variant }, ref) => {
  const theme = useTheme();
  const highlightColor = valueToColor(theme, value);

  return (
    <HoverNavigatable triggerNavigate={onClick}>
      <Button
        ref={ref}
        highlightColor={highlightColor}
        type="button"
        primary={variant === "primary"}
        size={size}
        selected={isSelected}
        onClick={onClick}
      >
        {children}
      </Button>
    </HoverNavigatable>
  );
});

export default SmallTabNavigationButton;
