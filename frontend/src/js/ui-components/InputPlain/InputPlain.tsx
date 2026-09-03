import type { Ref } from "react";
import { tv } from "tailwind-variants";

import type { CurrencyConfigT } from "../../api/types";
import BaseInput from "../BaseInput";
import Labeled from "../Labeled";

const input = tv({
  variants: {
    fullWidth: { true: "w-full" },
  },
});

interface Props {
  label: string;
  indexPrefix?: number;
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
  inputProps?: object;
  currencyConfig?: CurrencyConfigT;
  fullWidth?: boolean;
  tooltip?: string;
}

const InputPlain = ({
  ref,
  className,
  fullWidth,
  label,
  tinyLabel,
  large,
  indexPrefix,
  tooltip,
  inputType = "text",
  money,
  placeholder,
  value,
  onChange,
  onBlur,
  currencyConfig,
  inputProps,
}: Props & { ref?: Ref<HTMLInputElement> }) => {
  return (
    <Labeled
      className={className}
      fullWidth={fullWidth}
      label={label}
      tinyLabel={tinyLabel}
      largeLabel={large}
      indexPrefix={indexPrefix}
      tooltip={tooltip}
    >
      <BaseInput
        ref={ref}
        className={input({ fullWidth })}
        large={large}
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
