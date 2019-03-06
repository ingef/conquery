// @flow

import React from "react";
import styled from "@emotion/styled";
import { type FieldPropsType } from "redux-form";

import { isEmpty } from "../common/helpers";

import ClearableInput from "./ClearableInput";

const Root = styled("label")`
  width: ${({ fullWidth }) => (fullWidth ? "100%" : "initial")};

  input {
    width: ${({ fullWidth }) => fullWidth ? "100%" : "initial"};
    border: 1px solid
      ${({ theme, valueChanged }) =>
        valueChanged ? theme.col.blueGrayDark : "initial"};
  }
`;

const Label = styled("span")`
  font-size: ${({ theme, tiny }) => (tiny ? theme.font.xs : theme.font.sm)};
  display: ${({ inline }) => (inline ? "inline-block" : "block")};
  margin: ${({ inline }) => (inline ? "0" : "2px 8px")};
`;

type PropsType = FieldPropsType & {
  label: string,
  inputType: string,
  valueType?: string,
  className?: string,
  placeholder?: string,
  tinyLabel?: boolean,
  inputProps?: Object,
  fullWidth?: boolean
};

const InputWithLabel = (props: PropsType) => {
  return (
    <Root
      className={props.className}
      valueChanged={
        !isEmpty(props.input.value) &&
        props.input.value !== props.input.defaultValue
      }
      fullWidth={props.fullWidth}
    >
      <Label tiny={props.tinyLabel}>{props.label}</Label>
      <ClearableInput
        inputType={props.inputType}
        valueType={props.valueType}
        placeholder={props.placeholder}
        value={props.input.value || ""}
        formattedValue={props.input.formattedValue || ""}
        onChange={props.input.onChange}
        inputProps={props.inputProps}
      />
    </Root>
  );
};

export default InputWithLabel;
