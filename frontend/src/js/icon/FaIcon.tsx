import isPropValid from "@emotion/is-prop-valid";
import { keyframes } from "@emotion/react";
import styled from "@emotion/styled";
import { library } from "@fortawesome/fontawesome-svg-core";
import { far } from "@fortawesome/free-regular-svg-icons";
import { fas } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import React from "react";

library.add(fas, far);

export interface IconStyleProps {
  left?: boolean;
  center?: boolean;
  right?: boolean;
  white?: boolean;
  red?: boolean;
  light?: boolean;
  main?: boolean;
  active?: boolean;
  disabled?: boolean;
  tiny?: boolean;
  large?: boolean;
  small?: boolean;
}

export interface FaIconPropsT extends IconStyleProps {
  icon: string;
  className?: string;
  regular?: boolean;
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

export const Icon = styled(FontAwesomeIcon, {
  shouldForwardProp,
})<IconStyleProps>`
  padding-right: ${({ left }) => (left ? "10px" : "0")};
  padding-left: ${({ right }) => (right ? "10px" : "0")};
  text-align: ${({ center }) => (center ? "center" : "left")};
  font-size: ${({ theme, large, tiny }) =>
    large ? theme.font.md : tiny ? theme.font.tiny : theme.font.sm};
  color: ${({ theme, white, red, light, main, active, disabled }) =>
    disabled
      ? theme.col.grayMediumLight
      : active
      ? theme.col.blueGrayDark
      : white
      ? "#fff"
      : red
      ? theme.col.red
      : main
      ? theme.col.blueGray
      : light
      ? theme.col.blueGrayLight
      : theme.col.black};
  cursor: ${({ disabled }) => (disabled ? "not-allowed" : "inherit")};
  width: initial !important;

  &.fa-spinner {
    animation: ${spin} 0.5s linear 0s infinite;
  }
`;

const FaIcon: React.FC<FaIconPropsT> = ({
  icon,
  regular,
  className,
  ...restProps
}) => {
  return (
    <Icon
      className={`fa-fw ${className}`}
      icon={regular ? ["far", icon] : icon}
      {...restProps}
    />
  );
};

export default FaIcon;
