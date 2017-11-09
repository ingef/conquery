// @flow

import React                   from 'react';
import classnames              from 'classnames';
import { type FieldPropsType } from 'redux-form';

type OptionsType = {
  label: string,
  value: string
};

type PropsType = FieldPropsType & {
  options: OptionsType[],
};


const ToggleButton = (props: PropsType) => {
  return (
    <p className="toggle-button">
      {
        props.options.map(({ value, label }, i) => (
          <span
            key={i}
            className={classnames(
              'toggle-button__option',
              {
                'toggle-button__option--active': props.input.value === value,
              }
            )}
            onClick={() => {
              if (value !== props.input.value)
                props.input.onChange(value);
            }}
          >
            {label}
          </span>
        ))
      }
    </p>
  );
};

export default ToggleButton;
