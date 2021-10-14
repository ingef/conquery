import { css } from "@emotion/react";
import styled from "@emotion/styled";
import React from "react";

import type { CurrencyConfigT } from "../api/types";

import BaseInput from "./BaseInput";
import Labeled from "./Labeled";
import { InputProps } from "./types";

const SxBaseInput = styled(BaseInput)<{ fullWidth?: boolean }>`
  ${({ fullWidth }) =>
    fullWidth &&
    css`
      width: 100%;
    `};
`;

interface Props<T> extends InputProps<T> {
  label: string;
  indexPrefix?: number;
  optional?: boolean;
  inputType?: string;
  money?: boolean;
  className?: string;
  placeholder?: string;
  tinyLabel?: boolean;
  large?: boolean;
  inputProps?: Object;
  currencyConfig?: CurrencyConfigT;
  fullWidth?: boolean;
  tooltip?: string;
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
      indexPrefix={props.indexPrefix}
      tooltip={props.tooltip}
      optional={props.optional}
    >
      <SxBaseInput
        large={props.large}
        fullWidth={props.fullWidth}
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
