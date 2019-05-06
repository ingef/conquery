// @flow

import React from "react";
import { type FieldPropsType } from "redux-form";

import { isEmpty } from "../common/helpers";

import BaseInput from "./BaseInput";
import Labeled from "./Labeled";

type PropsType = FieldPropsType & {
  label: string,
  inputType?: string,
  valueType?: string,
  className?: string,
  placeholder?: string,
  tinyLabel?: boolean,
  inputProps?: Object,
  fullWidth?: boolean
};

const InputText = (props: PropsType) => {
  return (
    <Labeled
      className={props.className}
      valueChanged={
        !isEmpty(props.input.value) &&
        props.input.value !== props.input.defaultValue
      }
      fullWidth={props.fullWidth}
      label={props.label}
      tinyLabel={props.tinyLabel}
    >
      <BaseInput
        inputType={props.inputType || "text"}
        valueType={props.valueType}
        placeholder={props.placeholder}
        value={props.input.value || ""}
        formattedValue={props.input.formattedValue || ""}
        onChange={props.input.onChange}
        inputProps={props.inputProps}
      />
    </Labeled>
  );
};

export default InputText;
