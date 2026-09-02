import { useEffect, useState } from "react";
import {
  type InputAttributes,
  NumericFormat,
  type NumericFormatProps,
} from "react-number-format";
import { tv } from "tailwind-variants";

import type { CurrencyConfigT } from "../api/types";
import { isEmpty } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";

const numberFormat = tv({
  base: [
    "outline-0",
    "min-w-[170px]",
    "border border-gray-400",
    "rounded",
    "py-2 pr-[30px] pl-[10px]",
    "text-sm",
  ],
  variants: {
    large: { true: "py-[10px] pr-[30px] pl-[14px] text-xl" },
  },
});

const CurrencyInput = ({
  currencyConfig,
  value,
  onChange,
  placeholder,
  large,
}: {
  value: number | null;
  onChange: (parsed: number | null) => void;
  currencyConfig?: CurrencyConfigT;
  placeholder?: string;
  large?: boolean;
}) => {
  const factor = currencyConfig ? 10 ** currencyConfig.decimalScale : 1;
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
  // typed up front with an explicit base type: the polymorphic NumericFormat
  // props are otherwise too complex a union for tsc
  const props: NumericFormatProps = {
    ...currencyConfig,
    className: numberFormat({ large }),
    suffix: ` ${currencyConfig?.unit}`,
    placeholder,
    type: "text",
    value: numberFormatValue,
    onValueChange: (values) => {
      if (exists(values.floatValue) && !Number.isNaN(values.floatValue)) {
        setNumberFormatValue(values.floatValue);
        onChange(parseInt((values.floatValue * factor).toFixed(0), 10));
      } else {
        setNumberFormatValue("");
        onChange(null);
      }
    },
  };

  return <NumericFormat<InputAttributes> {...props} />;
};

export default CurrencyInput;
