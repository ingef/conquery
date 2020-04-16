import * as React from "react";
import NumberFormat from "react-number-format";

import { isEmpty } from "../common/helpers";
import type { CurrencyConfigT } from "../api/types";

type PropsType = {
  value: number | null;
  onChange: (parsed: number | null) => void;
  currencyConfig?: CurrencyConfigT;
  placeholder?: string;
};

// https://github.com/s-yadav/react-number-format#values-object
type NumberFormatValueType = {
  formattedValue: string;
  value: string;
  floatValue: number;
};

const CurrencyInput = ({
  currencyConfig,
  value,
  onChange,
  placeholder
}: PropsType) => {
  const factor = currencyConfig ? Math.pow(10, currencyConfig.decimalScale) : 1;
  // Super weird: In react-number-format,
  //   in order to properly display the placeholder, "-", the only way is to
  //   NOT supply isNumberString
  //   and instead to supply EITHER a float value OR an empty string
  const [formattedValue, setFormattedValue] = React.useState<number | string>(
    isEmpty(value) ? "" : value / factor
  );

  React.useEffect(() => {
    // If formatted is cleared from outside, reset
    if (isEmpty(value)) {
      setFormattedValue("");
    }
  }, [value]);

  function onValueChange(values: NumberFormatValueType) {
    const parsed =
      isEmpty(values.floatValue) || isNaN(values.floatValue)
        ? null
        : parseInt((values.floatValue * factor).toFixed(0), 10);

    setFormattedValue(values.formattedValue);

    onChange(parsed);
  }

  return (
    <NumberFormat
      {...currencyConfig}
      className="clearable-input__input"
      placeholder={placeholder}
      type="text"
      onValueChange={onValueChange}
      value={formattedValue}
    />
  );
};

export default CurrencyInput;
