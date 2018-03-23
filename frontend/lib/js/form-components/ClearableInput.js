// @flow

import React                from 'react';
import T                    from 'i18n-react';
import NumberFormat         from 'react-number-format';

import { isEmpty }          from '../common/helpers';
import { MONEY_RANGE }      from '.';

type PropsType = {
  inputType: string,
  valueType?: string,
  placeholder?: string,
  value: ?(number | string),
  formattedValue?: string,
  inputProps?: Object,
  onChange: Function,
};

const ClearableInput = (props: PropsType) => {
  return (
    <span className="clearable-input">
      {
        props.valueType === MONEY_RANGE
        ? <NumberFormat
            prefix={T.translate('moneyRange.prefix')}
            thousandSeparator={T.translate('moneyRange.thousandSeparator')}
            decimalSeparator={T.translate('moneyRange.decimalSeparator')}
            decimalScale={T.translate('moneyRange.decimalScale')}
            className="clearable-input__input"
            placeholder={props.placeholder}
            type={props.inputType}
            onValueChange={(values) => { // values: {floatValue, formattedValue, value}
              const { formattedValue, floatValue } = values;
              props.onChange({
                formattedValue,
                raw: floatValue * (T.translate('moneyRange.factor') || 1)
              });
            }}
            value={props.formattedValue}
            {...props.inputProps}
          />
        : <input
            className="clearable-input__input"
            placeholder={props.placeholder}
            type={props.inputType}
            onChange={(e) => props.onChange(e.target.value)}
            value={props.value}
            {...props.inputProps}
          />
      }
      {
        !isEmpty(props.value) &&
        <span
          className="clearable-input__clear-zone Select-clear-zone"
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
