import styled from "@emotion/styled";
import React, { FC, useState, useEffect } from "react";
import NumberFormat from "react-number-format";

import type { CurrencyConfigT } from "../api/types";
import { isEmpty } from "../common/helpers";
import { exists } from "../common/helpers/exists";

const SxNumberFormat = styled(NumberFormat)<{ large?: boolean }>`
  outline: 0;
  min-width: 170px;

  border: 1px solid ${({ theme }) => theme.col.grayMediumLight};
  font-size: ${({ theme }) => theme.font.md};
  padding: ${({ large }) =>
    large ? "10px 30px 10px 14px" : "8px 30px 8px 10px"};
  font-size: ${({ theme, large }) => (large ? theme.font.lg : theme.font.sm)};
  border-radius: ${({ theme }) => theme.borderRadius};
`;

interface PropsT {
  value: number | null;
  onChange: (parsed: number | null) => void;
  currencyConfig?: CurrencyConfigT;
  placeholder?: string;
  large?: boolean;
}

const CurrencyInput: FC<PropsT> = ({
  currencyConfig,
  value,
  onChange,
  placeholder,
  large,
}) => {
  const factor = currencyConfig ? Math.pow(10, currencyConfig.decimalScale) : 1;
  // Super weird: In react-number-format,
  //   in order to properly display the placeholder, "-", the only way is to
  //   NOT supply isNumberString
  //   and instead to supply EITHER a float value OR an empty string
  const [numberFormatValue, setNumberFormatValue] = useState<
    number | string | null
  >(exists(value) ? value / factor : null);

  useEffect(() => {
    // If formatted is cleared from outside, reset
    if (isEmpty(value)) {
      setNumberFormatValue("");
    }
  }, [value]);

  return (
    <SxNumberFormat
      {...currencyConfig}
      placeholder={placeholder}
      type="text"
      value={numberFormatValue}
      large={large}
      onValueChange={(values) => {
        if (exists(values.floatValue) && !isNaN(values.floatValue)) {
          setNumberFormatValue(values.floatValue);
          onChange(parseInt((values.floatValue * factor).toFixed(0), 10));
        } else {
          setNumberFormatValue("");
          onChange(null);
        }
      }}
    />
  );
};

export default CurrencyInput;
