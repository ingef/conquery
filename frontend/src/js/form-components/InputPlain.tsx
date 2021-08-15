import React from "react";

import type { CurrencyConfigT } from "../api/types";

import BaseInput from "./BaseInput";
import Labeled from "./Labeled";
import { InputProps } from "./types";

interface Props<T> extends InputProps<T> {
  label: string;
  inputType?: string;
  money?: boolean;
  className?: string;
  placeholder?: string;
  tinyLabel?: boolean;
  large?: boolean;
  inputProps?: Object;
  currencyConfig?: CurrencyConfigT;
  fullWidth?: boolean;
}

const InputPlain = <T extends string | number | null = string | null>(
  props: Props<T>,
) => {
  return (
    <Labeled
      className={props.className}
      fullWidth={props.fullWidth}
      label={props.label}
      tinyLabel={props.tinyLabel}
      largeLabel={props.large}
    >
      <BaseInput
        large={props.large}
        inputType={props.inputType || "text"}
        money={props.money}
        placeholder={props.placeholder}
        value={props.input.value}
        onChange={props.input.onChange}
        currencyConfig={props.currencyConfig}
        inputProps={props.inputProps}
      />
    </Labeled>
  );
};

export default InputPlain;
