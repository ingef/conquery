import React from "react";
import isPropValid from "@emotion/is-prop-valid";
import styled from "@emotion/styled";

import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { library } from "@fortawesome/fontawesome-svg-core";
import { far } from "@fortawesome/free-regular-svg-icons";
import { fas } from "@fortawesome/free-solid-svg-icons";

library.add(fas, far);

const shouldForwardProp = prop =>
  isPropValid(prop) || prop === "icon" || prop === "className";

export const Icon = styled(FontAwesomeIcon, { shouldForwardProp })`
  padding-right: ${({ left }) => (left ? "10px" : "0")};
  padding-left: ${({ right }) => (right ? "10px" : "0")};
  text-align: ${({ center }) => (center ? "center" : "left")};
  font-size: ${({ theme, large, tiny }) =>
    large ? theme.font.md : tiny ? theme.font.tiny : theme.font.sm};
  color: ${({ theme, white, light, main, active, disabled }) =>
    disabled
      ? theme.col.grayMediumLight
      : active
      ? theme.col.blueGrayDark
      : white
      ? "#fff"
      : main
      ? theme.col.blueGray
      : light
      ? theme.col.blueGrayLight
      : theme.col.black};
  cursor: ${({ disabled }) => (disabled ? "not-allowed" : "inherit")};
  width: initial !important;
`;

const FaIcon = ({ icon, regular, className, ...restProps }) => {
  return (
    <Icon
      className={`fa-fw ${className}`}
      icon={regular ? ["far", icon] : icon}
      {...restProps}
    />
  );
};

export default FaIcon;
