// @flow

import * as React from "react";
import styled from "@emotion/styled";

import Label from "./Label";

const Root = styled("label")`
  width: ${({ fullWidth }) => (fullWidth ? "100%" : "initial")};

  input {
    width: ${({ fullWidth }) => (fullWidth ? "100%" : "initial")};
    border: 1px solid
      ${({ theme, valueChanged }) =>
        valueChanged ? theme.col.blueGrayDark : "initial"};
  }
`;

type PropsType = {
  label: React.Node,
  className?: string,
  tinyLabel?: boolean,
  fullWidth?: boolean,
  valueChanged?: boolean,
  disabled?: boolean,
  children?: React.Node
};

const Labeled = (props: PropsType) => {
  return (
    <Root
      className={props.className}
      valueChanged={props.valueChanged}
      fullWidth={props.fullWidth}
      disabled={props.disabled}
    >
      <Label tiny={props.tinyLabel}>{props.label}</Label>
      {props.children}
    </Root>
  );
};

export default Labeled;
