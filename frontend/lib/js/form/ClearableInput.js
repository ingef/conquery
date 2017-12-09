// @flow

import React                from 'react';
import T                    from 'i18n-react';

import { isEmpty }          from '../common/helpers';

type PropsType = {
  inputType: string,
  placeholder?: string,
  value: ?(number | string),
  inputProps?: Object,
  onChange: Function,
};

const ClearableInput = (props: PropsType) => {
  return (
    <span className="clearable-input">
      <input
        className="clearable-input__input"
        placeholder={props.placeholder}
        type={props.inputType}
        onChange={(e) => props.onChange(e.target.value)}
        value={props.value}
        {...props.inputProps}
      />
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
