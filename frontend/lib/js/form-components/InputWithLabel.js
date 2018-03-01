// @flow

import React                   from 'react';
import classnames              from 'classnames';
import { type FieldPropsType } from 'redux-form';

import { isEmpty }             from '../common/helpers';

import ClearableInput          from './ClearableInput';

type PropsType = FieldPropsType & {
  label: string,
  inputType: string,
  className?: string,
  placeholder?: string,
  tinyLabel?: boolean,
  inputProps?: Object,
  fullWidth?: boolean,
};

const InputWithLabel = (props: PropsType) => {
  return (
    <label className={classnames(
      props.className,
      'input',
      {
        'input--value-changed':
          !isEmpty(props.input.value) && props.input.value !== props.input.defaultValue,
        'input--full-width': !!props.fullWidth,
      }
    )}>
      <span className={classnames(
        "input-label", {
          "input-label--tiny": !!props.tinyLabel
        }
      )}>
        {props.label}
      </span>
      <ClearableInput
        inputType={props.inputType}
        placeholder={props.placeholder}
        value={props.input.value || ""}
        onChange={props.input.onChange}
        inputProps={props.inputProps}
      />
    </label>
  );
};

export default InputWithLabel;
