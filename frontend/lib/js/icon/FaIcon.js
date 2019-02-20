import React from "react";
import styled from "@emotion/styled";

export const Icon = styled("div")`
  padding-right: ${({ left }) => (left ? "10px" : "0")};
  padding-left: ${({ right }) => (right ? "10px" : "0")};
  text-align: ${({ center }) => (center ? "center" : "left")};
  font-size: ${({ theme, large }) => (large ? theme.font.md : theme.font.sm)};
  color: ${({ theme, white, light, main, active }) =>
    active
      ? theme.col.blueGrayDark
      : white
      ? "#fff"
      : main
      ? theme.col.blueGray
      : light
      ? theme.col.blueGrayLight
      : theme.col.black};
  cursor: ${({ disabled }) => (disabled ? "not-allowed" : "inherit")};
`;

const FaIcon = ({ icon, className, ...restProps }) => {
  return <Icon className={`fa fa-${icon} ${className}`} {...restProps} />;
};

export default FaIcon;
