import isPropValid from "@emotion/is-prop-valid";
import type { Theme } from "@emotion/react";
import { keyframes } from "@emotion/react";
import styled from "@emotion/styled";
import type { IconProp } from "@fortawesome/fontawesome-svg-core";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { Ref } from "react";

export interface IconStyleProps {
  left?: boolean;
  center?: boolean;
  right?: boolean;
  white?: boolean;
  red?: boolean;
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

// First matching flag wins, in this order
const iconColor = (theme: Theme, props: IconStyleProps) => {
  if (props.disabled) return theme.col.grayMediumLight;
  if (props.red) return theme.col.red;
  if (props.gray) return theme.col.gray;
  if (props.active) return theme.col.blueGrayDark;
  if (props.white) return "#fff";
  if (props.light) return theme.col.blueGrayLight;
  if (props.main) return theme.col.blueGray;
  return theme.col.black;
};

// @ts-ignore TODO: Figure out how to avoid a type error with styled here
export const Icon = styled(FontAwesomeIcon, {
  shouldForwardProp,
})<IconStyleProps>`
  padding-right: ${({ left }) => (left ? "10px" : "0")};
  padding-left: ${({ right }) => (right ? "10px" : "0")};
  text-align: ${({ center }) => (center ? "center" : "left")};
  font-size: ${({ theme, large, tiny }) =>
    large ? theme.font.md : tiny ? theme.font.tiny : theme.font.sm};
  color: ${({ theme, ...props }) => iconColor(theme, props)};
  cursor: ${({ disabled }) => (disabled ? "not-allowed" : "inherit")};
  width: initial !important;

  &.fa-spinner {
    animation: ${spin} 0.5s linear 0s infinite;
  }
`;

const FaIcon = ({
  ref,
  icon,
  className,
  ...restProps
}: FaIconPropsT & { ref?: Ref<SVGSVGElement> }) => {
  return (
    <Icon
      // @ts-ignore TODO: ref is working, try fixing the type error
      ref={ref}
      className={`fa-fw ${className}`}
      icon={icon}
      {...restProps}
    />
  );
};

export default FaIcon;
