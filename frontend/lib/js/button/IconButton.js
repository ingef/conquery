// @flow

import React from "react";
import classnames from "classnames";

type PropsType = {
  label?: string,
  onClick?: Function,
  className?: string,
  iconClassName?: string,
  disabled?: boolean
};

// A button that is prefixed by an icon
const IconButton = ({
  onClick,
  label,
  className,
  iconClassName,
  disabled
}: PropsType) => (
  <button
    type="button"
    className={classnames("btn", className)}
    onClick={onClick}
    disabled={disabled}
  >
    <i className={classnames("fa", iconClassName)} /> {label}
  </button>
);

export default IconButton;
