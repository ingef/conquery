// @flow

import React from "react";
import styled from "@emotion/styled";

import IconButton from "../button/IconButton";

import { type FieldPropsType } from "redux-form";

const Label = styled("span")`
  margin-left: 10px;
`;

type PropsType = FieldPropsType & {
  label: string,
  className?: string,
  tinyLabel?: boolean
};

const InputCheckbox = (props: PropsType) => (
  <IconButton
    icon={props.input.value ? "check-square-o" : "square-o"}
    className={props.className}
    onClick={() => props.input.onChange(!props.input.value)}
  >
    <Label>{props.label}</Label>
  </IconButton>
);

export default InputCheckbox;
