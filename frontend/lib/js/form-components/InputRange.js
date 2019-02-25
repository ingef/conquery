// @flow

import React                    from 'react';
import T                        from 'i18n-react';
import classnames               from 'classnames';
import { type FieldPropsType }  from 'redux-form';

import { CurrencyType }         from '../standard-query-editor/types';

import InputWithLabel           from './InputWithLabel';
import ToggleButton             from './ToggleButton';
import InputRangeHeader         from './InputRangeHeader';

type PropsType = FieldPropsType & {
  inputType: string,
  valueType?: string,
  label: string,
  unit?: string,
  limits?: {
    min?: number,
    max?: number,
  },
  disabled: boolean,
  mode: 'range' | 'exact',
  stepSize?: number,
  smallLabel?: boolean,
  placeholder: string,
  onSwitchMode: Function,
  tooltip?: string,
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
    },
    pattern?: string
  },
  currencyConfig?: CurrencyType
};

const InputRange = (props: PropsType) => {
  const { value, formattedValue } = props.input;
  // Make sure undefined / null is never set as a value, but an empty string instead
  const minValue = (value && value.min) || '';
  const maxValue = (value && value.max) || '';
  const exactValue = (value && value.exact) || '';

  const factor = (props.currencyConfig && props.currencyConfig.factor) || 1;
  const minFormattedValue =
    (formattedValue && formattedValue.min) || (Math.round(minValue) / factor) || null;
  const maxFormattedValue =
    (formattedValue && formattedValue.max) || (Math.round(maxValue) / factor) || null;
  const exactFormattedValue =
    (formattedValue && formattedValue.exact) || (Math.round(exactValue) / factor) || null;

  const isRangeMode = props.mode === 'range';
  const inputProps = {
    step: props.stepSize || null,
    min: (props.limits && props.limits.min) || null,
    max: (props.limits && props.limits.max) || null,
    currency: props.currencyConfig,
    pattern: props.pattern
  };

  const onChangeValue = (type, newValue, newFormattedValue) => {
    const { value } = props.input;
    const nextValue = newValue >= 0 ? newValue : null;
    const nextFormattedValue = newFormattedValue || null;

    if (type === 'exact')
      // SET ENTIRE VALUE TO NULL IF POSSIBLE
      if (nextValue === null)
        props.input.onChange(null, null);
      else
        props.input.onChange({
          exact: nextValue
        }, {
          exact: nextFormattedValue
        });
    else if (type === 'min' || type === 'max')
    if (
      nextValue === null && (
        (value && value.min == null && type === 'max') ||
        (value && value.max == null && type === 'min')
      )
    )
      props.input.onChange(null, null);
    else
        props.input.onChange({
          min: value ? value.min : null,
          max: value ? value.max : null,
          [type]: nextValue
        },
        {
          min: props.input.formattedValue ? props.input.formattedValue.min : null,
          max: props.input.formattedValue ? props.input.formattedValue.max : null,
          [type]: nextFormattedValue
        });
    else
      props.input.onChange(null, null);
  };

  return (
    <div>
      <InputRangeHeader
        label={props.label}
        unit={props.unit}
        tooltip={props.tooltip}
        className={classnames(
          'input-label', {
            'input-label--disabled': props.disabled
          }
        )}
      />
      <ToggleButton
        input={{
          value: props.mode || "range",
          onChange: (mode) => props.onSwitchMode(mode),
        }}
        options={[
          { value: 'range', label: T.translate('inputRange.range') },
          { value: 'exact', label: T.translate('inputRange.exact') },
        ]}
      />
      {
        !isRangeMode &&
        <div className="input-range__input-container">
          <InputWithLabel
            inputType={props.inputType}
            valueType={props.valueType}
            className="input-range__input-with-label"
            placeholder="-"
            label={T.translate('inputRange.exactLabel')}
            tinyLabel={props.smallLabel || true}
            input={{
              value: exactValue,
              formattedValue: exactFormattedValue,
              onChange: (value, formattedValue) => onChangeValue('exact', value, formattedValue)
            }}
            inputProps={inputProps}
          />
        </div>
      }
      {
        isRangeMode &&
        <div className="input-range__input-container">
          <InputWithLabel
            inputType={props.inputType}
            valueType={props.valueType}
            className="input-range__input-with-label"
            placeholder={props.placeholder}
            label={T.translate('inputRange.minLabel')}
            tinyLabel={props.smallLabel || true}
            input={{
              value: minValue,
              formattedValue: minFormattedValue,
              onChange: (value, formattedValue) => onChangeValue('min', value, formattedValue),
            }}
            inputProps={inputProps}
          />
          <InputWithLabel
            inputType={props.inputType}
            valueType={props.valueType}
            className="input-range__input-with-label"
            placeholder={props.placeholder}
            label={T.translate('inputRange.maxLabel')}
            tinyLabel={props.smallLabel || true}
            input={{
              value: maxValue,
              formattedValue: maxFormattedValue,
              onChange: (value, formattedValue) => onChangeValue('max', value, formattedValue),
            }}
            inputProps={inputProps}
          />
        </div>
      }
    </div>
  );
};

export default InputRange;
