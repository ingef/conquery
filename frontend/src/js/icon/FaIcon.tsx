import isPropValid from "@emotion/is-prop-valid";
import { keyframes } from "@emotion/react";
import styled from "@emotion/styled";
import { IconProp } from "@fortawesome/fontawesome-svg-core";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { forwardRef } from "react";

export interface IconStyleProps {
  left?: boolean;
  center?: boolean;
  right?: boolean;
  white?: boolean;
  light?: boolean;
  gray?: boolean;
  main?: boolean;
  active?: boolean;
  disabled?: boolean;
  tiny?: boolean;
  large?: boolean;
  small?: boolean;
  style?: React.CSSProperties;
}

export interface FaIconPropsT extends IconStyleProps {
  icon: IconProp;
  className?: string;
}

const spin = keyframes`
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
`;

const shouldForwardProp = (prop: keyof FaIconPropsT) =>
  isPropValid(prop) || prop === "icon" || prop === "className";

// TODO: Figure out how to avoid a type error with styled here
// @ts-ignore
export const Icon = styled(FontAwesomeIcon, {
  shouldForwardProp,
})<IconStyleProps>`
  padding-right: ${({ left }) => (left ? "10px" : "0")};
  padding-left: ${({ right }) => (right ? "10px" : "0")};
  text-align: ${({ center }) => (center ? "center" : "left")};
  font-size: ${({ theme, large, tiny }) =>
    large ? theme.font.md : tiny ? theme.font.tiny : theme.font.sm};
  color: ${({ theme, white, gray, light, main, active, disabled }) =>
    disabled
      ? theme.col.grayMediumLight
      : gray
      ? theme.col.gray
      : active
      ? theme.col.blueGrayDark
      : white
      ? "#fff"
      : light
      ? theme.col.blueGrayLight
      : main
      ? theme.col.blueGray
      : theme.col.black};
  cursor: ${({ disabled }) => (disabled ? "not-allowed" : "inherit")};
  width: initial !important;

  &.fa-spinner {
    animation: ${spin} 0.5s linear 0s infinite;
  }
`;

const FaIcon = forwardRef<SVGSVGElement, FaIconPropsT>(
  ({ icon, className, ...restProps }, ref) => {
    return (
      <Icon
        // TODO: ref is working, try fixing the type error
        // @ts-ignore
        ref={ref}
        className={`fa-fw ${className}`}
        icon={icon}
        {...restProps}
      />
    );
  },
);

export default FaIcon;
