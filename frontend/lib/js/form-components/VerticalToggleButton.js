// @flow

import React                from 'react';
import classnames           from 'classnames';

type PropsType = {
  onToggle: string => void,
  activeValue: string,
  options: {
    label: string,
    value: string,
  }[]
};

const VerticalToggleButton = (props: PropsType) => {
  return (
    <p className="vertical-toggle-button">
      {
        props.options.map(({ value, label }, i) => (
          <span
            key={i}
            className={classnames(
              'vertical-toggle-button__option',
              {
                'vertical-toggle-button__option--active': props.activeValue === value,
              }
            )}
            onClick={() => {
              if (value !== props.activeValue)
                props.onToggle(value);
            }}
          >
            {label}
          </span>
        ))
      }
    </p>
  );
};

export default VerticalToggleButton;
