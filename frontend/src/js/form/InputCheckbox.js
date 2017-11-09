// @flow

import React                from 'react';
import classnames           from 'classnames';

import { type FieldPropsType } from 'redux-form';

type PropsType = FieldPropsType & {
  label: string,
  className?: string,
  tinyLabel?: boolean,
};

const InputCheckbox = (props: PropsType) => (
  <button
    type="button"
    className={classnames(
      props.className,
      'input-checkbox',
      'btn',
      'btn--transparent'
    )}
    onClick={() => props.input.onChange(!props.input.value)}
  >
    <i className={classnames(
      'fa', {
        'fa-square-o': !props.input.value,
        'fa-check-square-o': props.input.value
      }
    )} />
    <span className="input-checkbox__label">
      {props.label}
    </span>
  </button>
);

export default InputCheckbox;
