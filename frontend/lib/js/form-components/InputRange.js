// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { type FieldPropsType } from "redux-form";

import type { CurrencyType } from "../standard-query-editor/types";

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
  smallLabel?: boolean,
  placeholder: string,
  onSwitchMode: Function,
  tooltip?: string,
  pattern?: string,
  input: {
    value: ?{
      exact?: number,
      min?: number,
      max?: number
    },
    formattedValue: ?{
      exact?: string,
      min?: string,
      max?: string
    }
  },
  currencyConfig?: CurrencyType
};

function getMinMaxExact(value) {
  if (!value) return { min: "", max: "", exact: "" };

  return {
    min: value.min || "",
    max: value.max || "",
    exact: value.exact || ""
  };
}

function getFormattedMinMaxExact({ min, max, exact }, formatted, factor) {
  if (!formatted) return { min: "", max: "", exact: "" };

  return {
    min: formatted.min || Math.round(min) / factor || null,
    max: formatted.max || Math.round(max) / factor || null,
    exact: formatted.exact || Math.round(exact) / factor || null
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
    smallLabel,
    disabled,
    label,
    unit,
    tooltip,
    onSwitchMode,
    input: { value, formattedValue, onChange }
  } = props;
  // Make sure undefined / null is never set as a value, but an empty string instead
  const val = getMinMaxExact(value);
  const factor = (currencyConfig && currencyConfig.factor) || 1;
  const formattedVal = getFormattedMinMaxExact(val, formattedValue, factor);
  const isRangeMode = mode === "range";

  const inputProps = {
    step: stepSize || null,
    min: (limits && limits.min) || null,
    max: (limits && limits.max) || null,
    currency: currencyConfig,
    pattern: pattern
  };

  const onChangeValue = (type, newValue, newFormattedValue) => {
    const nextValue = newValue >= 0 ? newValue : null;
    const nextFormattedValue = newFormattedValue || null;

    if (type === "exact")
      if (nextValue === null)
        // SET ENTIRE VALUE TO NULL IF POSSIBLE
        onChange(null, null);
      else onChange({ exact: nextValue }, { exact: nextFormattedValue });
    else if (type === "min" || type === "max")
      if (
        nextValue === null &&
        ((value && value.min == null && type === "max") ||
          (value && value.max == null && type === "min"))
      )
        onChange(null, null);
      else
        onChange(
          {
            min: value ? value.min : null,
            max: value ? value.max : null,
            [type]: nextValue
          },
          {
            min: formattedValue ? formattedValue.min : null,
            max: formattedValue ? formattedValue.max : null,
            [type]: nextFormattedValue
          }
        );
    else onChange(null, null);
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
              valueType={valueType}
              placeholder={placeholder}
              label={T.translate("inputRange.minLabel")}
              tinyLabel={smallLabel || true}
              input={{
                value: val.min,
                formattedValue: formattedVal.min,
                onChange: (value, formattedValue) =>
                  onChangeValue("min", value, formattedValue)
              }}
              inputProps={inputProps}
            />
            <StyledInputText
              inputType={inputType}
              valueType={valueType}
              placeholder={placeholder}
              label={T.translate("inputRange.maxLabel")}
              tinyLabel={smallLabel || true}
              input={{
                value: val.max,
                formattedValue: formattedVal.max,
                onChange: (value, formattedValue) =>
                  onChangeValue("max", value, formattedValue)
              }}
              inputProps={inputProps}
            />
          </>
        ) : (
          <InputText
            inputType={inputType}
            valueType={valueType}
            placeholder="-"
            label={T.translate("inputRange.exactLabel")}
            tinyLabel={smallLabel || true}
            input={{
              value: val.exact,
              formattedValue: formattedVal.exact,
              onChange: (value, formattedValue) =>
                onChangeValue("exact", value, formattedValue)
            }}
            inputProps={inputProps}
          />
        )}
      </Container>
    </div>
  );
};

export default InputRange;
