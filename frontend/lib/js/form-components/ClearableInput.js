// @flow

import React                from 'react';
import T                    from 'i18n-react';
import NumberFormat         from 'react-number-format';

import { isEmpty }          from '../common/helpers';
import { MONEY_RANGE }      from './filterTypes';

type PropsType = {
  inputType: string,
  valueType?: string,
  placeholder?: string,
  value: ?(number | string),
  formattedValue?: string,
  inputProps?: Object,
  onChange: Function,
};

type NumberFormatValueType = {
  floatValue:number,
  formattedValue: string,
  value: string
};

const ClearableInput = (props: PropsType) => {
  const { currency, pattern } = props.inputProps || {};

  const handleKeyPress = (event) => {
    var regex = new RegExp(pattern);
    var key = String.fromCharCode(!event.charCode ? event.which : event.charCode);
    if (!regex.test(key)) {
       event.preventDefault();
       return false;
    }
  }

  return (
    <span className="clearable-input">
      {
        props.valueType === MONEY_RANGE
        ? <NumberFormat
            prefix={currency.prefix || ''}
            thousandSeparator={currency.thousandSeparator || ''}
            decimalSeparator={currency.decimalSeparator || ''}
            decimalScale={currency.decimalScale || ''}
            className="clearable-input__input"
            placeholder={props.placeholder}
            type={props.inputType}
            onValueChange={(values: NumberFormatValueType) => {
              const { formattedValue, floatValue } = values;
              const parsed = Math.round(floatValue * (currency.factor || 1))

              props.onChange(parsed, formattedValue);
            }}
            value={props.formattedValue}
            {...props.inputProps}
          />
        : <input
            className="clearable-input__input"
            placeholder={props.placeholder}
            type={props.inputType}
            onChange={(e) => props.onChange(e.target.value)}
            onKeyPress={(e) => handleKeyPress(e)}
            value={props.value}
            {...props.inputProps}
          />
      }
      {
        !isEmpty(props.value) &&
        <span
          className="clearable-input__clear-zone"
          title={T.translate('common.clearValue')}
          aria-label={T.translate('common.clearValue')}
          onClick={() => props.onChange('')}
        >
          ×
        </span>
      }
    </span>
  );
};

export default ClearableInput;
