import React from "react";
import type { WrappedFieldProps } from "redux-form";

import { isEmpty } from "../common/helpers";

import BaseInput from "./BaseInput";
import Labeled from "./Labeled";

import type { CurrencyConfigT } from "../api/types";

interface PropsT extends WrappedFieldProps {
  label: string;
  inputType?: string;
  valueType?: string;
  className?: string;
  placeholder?: string;
  tinyLabel?: boolean;
  large?: boolean;
  inputProps?: Object;
  currencyConfig?: CurrencyConfigT;
  fullWidth?: boolean;
}

const InputText: React.FC<PropsT> = (props) => {
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
      largeLabel={props.large}
    >
      <BaseInput
        large={props.large}
        inputType={props.inputType || "text"}
        valueType={props.valueType}
        placeholder={props.placeholder}
        value={props.input.value}
        onChange={props.input.onChange}
        currencyConfig={props.currencyConfig}
        inputProps={props.inputProps}
      />
    </Labeled>
  );
};

export default InputText;
