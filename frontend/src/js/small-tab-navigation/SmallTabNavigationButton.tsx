import { useTheme, Theme, css } from "@emotion/react";
import styled from "@emotion/styled";
import { forwardRef } from "react";
import { HoverNagiatable } from "./HoverNavigatable";

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
}>`
  position: relative;
  border: 0;
  background-color: transparent;
  margin: 0 2px;
  height: 26px;
  padding: ${({ size }) => (size === "L" ? "0px 6px" : "0px 3px")};
  font-size: ${({ theme, size }) =>
    size === "L" ? theme.font.sm : theme.font.xs};

  ${({ size }) =>
    size === "M" &&
    css`
      text-transform: uppercase;
    `};

  ${({ selected, highlightColor }) =>
    selected &&
    css`
      &::after {
        ${bottomBorderBase};
        background-color: ${highlightColor};
      }
    `}
  ${({ theme, selected }) =>
    !selected &&
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

interface PropsT {
  value: string;
  size: "M" | "L";
  isSelected?: boolean;
  onClick: () => void;
  children?: React.ReactNode;
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

const SmallTabNavigationButton = forwardRef<HTMLButtonElement, PropsT>(
  ({ value, children, size, isSelected, onClick }, ref) => {
    const theme = useTheme();
    const highlightColor = valueToColor(theme, value);

    return (
      <HoverNagiatable triggerNavigate={onClick}      
      >
        <Button
          ref={ref}
          highlightColor={highlightColor}
          type="button"
          size={size}
          selected={isSelected}
          onClick={onClick}
        >
          {children}
        </Button>
      </HoverNagiatable>
    );
  },
);

export default SmallTabNavigationButton;
