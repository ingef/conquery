import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import type { FieldPropsType } from "redux-form";

import type { CurrencyConfigT } from "../api/types";

import InputText from "./InputText";
import ToggleButton from "./ToggleButton";
import InputRangeHeader from "./InputRangeHeader";

const Container = styled("div")`
  width: 100%;
  display: flex;
  flex-direction: row;
`;

const StyledInputText = styled(InputText)`
  input {
    width: 100%;
    min-width: 50px;
  }
  &:last-of-type {
    padding-left: 5px;
  }
`;

type PropsType = FieldPropsType & {
  inputType: string,
  valueType?: string,
  label: string,
  unit?: string,
  limits?: {
    min?: number,
    max?: number
  },
  disabled: boolean,
  mode: "range" | "exact",
  stepSize?: number,
  placeholder: string,
  onSwitchMode: Function,
  tooltip?: string,
  pattern?: string,
  input: {
    value: {
      exact?: number,
      min?: number,
      max?: number
    } | null
  },
  currencyConfig?: CurrencyConfigT
};

function getMinMaxExact(value) {
  if (!value) return { min: "", max: "", exact: "" };

  return {
    min: value.min || "",
    max: value.max || "",
    exact: value.exact || ""
  };
}

const InputRange = (props: PropsType) => {
  const {
    limits,
    stepSize,
    currencyConfig,
    pattern,
    mode,
    inputType,
    valueType,
    placeholder,
    disabled,
    label,
    unit,
    tooltip,
    onSwitchMode,
    input: { value, onChange }
  } = props;
  // Make sure undefined / null is never set as a value, but an empty string instead
  const val = getMinMaxExact(value);
  const isRangeMode = mode === "range";

  const inputProps = {
    step: stepSize || null,
    min: (limits && limits.min) || null,
    max: (limits && limits.max) || null,
    pattern: pattern
  };

  const onChangeValue = (type, newValue) => {
    const nextValue = newValue >= 0 ? newValue : null;

    if (type === "exact")
      if (nextValue === null) onChange(null);
      else onChange({ exact: nextValue });
    else if (type === "min" || type === "max")
      if (
        nextValue === null &&
        ((value && value.min === null && type === "max") ||
          (value && value.max === null && type === "min"))
      )
        onChange(null);
      else
        onChange({
          min: value ? value.min : null,
          max: value ? value.max : null,
          [type]: nextValue
        });
    else onChange(null);
  };

  return (
    <div>
      <InputRangeHeader
        disabled={disabled}
        label={label}
        unit={unit}
        tooltip={tooltip}
      />
      <ToggleButton
        input={{
          value: mode || "range",
          onChange: mode => onSwitchMode(mode)
        }}
        options={[
          { value: "range", label: T.translate("inputRange.range") },
          { value: "exact", label: T.translate("inputRange.exact") }
        ]}
      />
      <Container>
        {isRangeMode ? (
          <>
            <StyledInputText
              inputType={inputType}
              currencyConfig={currencyConfig}
              valueType={valueType}
              placeholder={placeholder}
              label={T.translate("inputRange.minLabel")}
              tinyLabel={true}
              input={{
                value: val.min,
                onChange: value => onChangeValue("min", value)
              }}
              inputProps={inputProps}
            />
            <StyledInputText
              inputType={inputType}
              currencyConfig={currencyConfig}
              valueType={valueType}
              placeholder={placeholder}
              label={T.translate("inputRange.maxLabel")}
              tinyLabel={true}
              input={{
                value: val.max,
                onChange: value => onChangeValue("max", value)
              }}
              inputProps={inputProps}
            />
          </>
        ) : (
          <InputText
            inputType={inputType}
            currencyConfig={currencyConfig}
            valueType={valueType}
            placeholder="-"
            label={T.translate("inputRange.exactLabel")}
            tinyLabel={true}
            input={{
              value: val.exact,
              onChange: value => onChangeValue("exact", value)
            }}
            inputProps={inputProps}
          />
        )}
      </Container>
    </div>
  );
};

export default InputRange;
