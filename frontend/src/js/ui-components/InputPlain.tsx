import { css } from "@emotion/react";
import styled from "@emotion/styled";

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

interface Props<T> {
  label: string;
  indexPrefix?: number;
  optional?: boolean;
  inputType?: string;
  money?: boolean;
  className?: string;
  placeholder?: string;
  tinyLabel?: boolean;
  large?: boolean;
  value: T;
  onChange: (value: T) => void;
  onBlur: () => void;
  defaultValue?: T;
  inputProps?: Object;
  currencyConfig?: CurrencyConfigT;
  fullWidth?: boolean;
  tooltip?: string;
}

const InputPlain = <T extends string | number | null = string | null>({
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
}: Props<T>) => {
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
};

export default InputPlain;
