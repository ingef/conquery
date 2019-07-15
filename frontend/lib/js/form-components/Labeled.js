// @flow

import * as React from "react";
import styled from "@emotion/styled";
import { css } from "@emotion/core";

import Label from "./Label";

const Root = styled("label")`
  ${({ fullWidth }) =>
    fullWidth &&
    css`
      width: 100%;
      input {
        width: 100%;
      }
    `};
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
