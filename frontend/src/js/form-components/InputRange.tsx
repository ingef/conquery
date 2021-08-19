import styled from "@emotion/styled";
import React from "react";
import { useTranslation } from "react-i18next";

import type { CurrencyConfigT } from "../api/types";
import { exists } from "../common/helpers/exists";

import InputPlain from "./InputPlain";
import InputRangeHeader from "./InputRangeHeader";
import ToggleButton from "./ToggleButton";
import { InputProps } from "./types";

const Container = styled("div")`
  width: 100%;
  display: flex;
  flex-direction: row;
  margin-top: -3px;
`;

const SxInputPlain = styled(InputPlain)`
  input {
    width: 100%;
    min-width: 50px;
  }
  &:last-of-type {
    padding-left: 5px;
  }
`;

interface ValueT {
  min?: number | null;
  max?: number | null;
  exact?: number | null;
}

export type ModeT = "range" | "exact";
interface PropsType extends InputProps<ValueT | null> {
  moneyRange?: boolean;
  label: string;
  indexPrefix?: number;
  unit?: string;
  limits?: {
    min?: number;
    max?: number;
  };
  disabled: boolean;
  mode: "range" | "exact";
  stepSize?: number;
  placeholder: string;
  onSwitchMode: (mode: ModeT) => void;
  tooltip?: string;
  pattern?: string;
  currencyConfig?: CurrencyConfigT;
}

function getMinMaxExact(value: ValueT | null) {
  if (!value) return { min: null, max: null, exact: null };

  return {
    min: exists(value.min) ? value.min : null,
    max: exists(value.max) ? value.max : null,
    exact: exists(value.exact) ? value.exact : null,
  };
}

const InputRange = ({
  limits,
  stepSize,
  currencyConfig,
  pattern,
  mode,
  moneyRange,
  placeholder,
  disabled,
  label,
  indexPrefix,
  unit,
  tooltip,
  onSwitchMode,
  input: { value, defaultValue, onChange },
}: PropsType) => {
  const { t } = useTranslation();
  // Make sure undefined / null is never set as a value, but an empty string instead
  const val = getMinMaxExact(value);
  const defaultVal = defaultValue || {};
  const isRangeMode = mode === "range";

  const inputProps = {
    step: stepSize || null,
    min: (limits && limits.min) || null,
    max: (limits && limits.max) || null,
    pattern: pattern,
  };

  const onChangeValue = (type: "exact" | "max" | "min", newValue: number) => {
    const nextValue = newValue >= 0 ? newValue : null;

    if (type === "exact") {
      if (nextValue === null) {
        onChange(null);
      } else {
        onChange({ exact: nextValue });
      }
    } else if (type === "min" || type === "max") {
      if (
        nextValue === null &&
        ((value && value.min === null && type === "max") ||
          (value && value.max === null && type === "min"))
      ) {
        onChange(null);
      } else {
        onChange({
          min: value ? value.min : null,
          max: value ? value.max : null,
          [type]: nextValue,
        });
      }
    } else {
      onChange(null);
    }
  };

  return (
    <div>
      <InputRangeHeader
        disabled={disabled}
        label={label}
        indexPrefix={indexPrefix}
        unit={unit}
        tooltip={tooltip}
      />
      <ToggleButton
        input={{
          value: mode || "range",
          onChange: (mode) => onSwitchMode(mode),
        }}
        options={[
          { value: "range", label: t("inputRange.range") },
          { value: "exact", label: t("inputRange.exact") },
        ]}
      />
      <Container>
        {isRangeMode ? (
          <>
            <SxInputPlain<number>
              inputType="number"
              currencyConfig={currencyConfig}
              money={moneyRange}
              placeholder={placeholder}
              label={t("inputRange.minLabel")}
              tinyLabel={true}
              input={{
                value: val.min,
                defaultValue: defaultVal.min,
                onChange: (value) => onChangeValue("min", value),
              }}
              inputProps={inputProps}
            />
            <SxInputPlain<number>
              inputType="number"
              currencyConfig={currencyConfig}
              money={moneyRange}
              placeholder={placeholder}
              label={t("inputRange.maxLabel")}
              tinyLabel={true}
              input={{
                value: val.max,
                defaultValue: defaultVal.max,
                onChange: (value) => onChangeValue("max", value),
              }}
              inputProps={inputProps}
            />
          </>
        ) : (
          <InputPlain<number>
            inputType="number"
            currencyConfig={currencyConfig}
            money={moneyRange}
            placeholder={placeholder}
            label={t("inputRange.exactLabel")}
            tinyLabel={true}
            input={{
              value: val.exact,
              defaultValue: defaultVal.exact,
              onChange: (value) => onChangeValue("exact", value),
            }}
            inputProps={inputProps}
          />
        )}
      </Container>
    </div>
  );
};

export default InputRange;
