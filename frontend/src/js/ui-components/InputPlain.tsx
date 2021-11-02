import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { forwardRef } from "react";

import type { CurrencyConfigT } from "../api/types";

import BaseInput from "./BaseInput";
import Labeled from "./Labeled";

const SxBaseInput = styled(BaseInput)<{ fullWidth?: boolean }>`
  ${({ fullWidth }) =>
    fullWidth &&
    css`
      width: 100%;
    `};
`;

interface Props {
  label: string;
  indexPrefix?: number;
  optional?: boolean;
  inputType?: string;
  money?: boolean;
  className?: string;
  placeholder?: string;
  tinyLabel?: boolean;
  large?: boolean;
  value: string | number | null;
  onChange: (value: string | number | null) => void;
  onBlur?: () => void;
  defaultValue?: string | number | null;
  inputProps?: Object;
  currencyConfig?: CurrencyConfigT;
  fullWidth?: boolean;
  tooltip?: string;
}

const InputPlain = forwardRef<HTMLInputElement, Props>(
  (
    {
      className,
      fullWidth,
      label,
      tinyLabel,
      large,
      indexPrefix,
      tooltip,
      optional,
      inputType = "text",
      money,
      placeholder,
      value,
      onChange,
      onBlur,
      currencyConfig,
      inputProps,
    },
    ref,
  ) => {
    return (
      <Labeled
        className={className}
        fullWidth={fullWidth}
        label={label}
        tinyLabel={tinyLabel}
        largeLabel={large}
        indexPrefix={indexPrefix}
        tooltip={tooltip}
        optional={optional}
      >
        <SxBaseInput
          ref={ref}
          large={large}
          fullWidth={fullWidth}
          inputType={inputType}
          money={money}
          placeholder={placeholder}
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          currencyConfig={currencyConfig}
          inputProps={inputProps}
        />
      </Labeled>
    );
  },
);

export default InputPlain;
