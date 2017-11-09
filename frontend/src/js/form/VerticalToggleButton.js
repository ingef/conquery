import React, { PropTypes } from 'react';
import classnames           from 'classnames';

const VerticalToggleButton = (props) => {
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

VerticalToggleButton.propTypes = {
  onToggle: PropTypes.func.isRequired,
  activeValue: PropTypes.string.isRequired,
  options: PropTypes.arrayOf(PropTypes.shape({
    label: PropTypes.string,
    value: PropTypes.string,
  })).isRequired,
};

export default VerticalToggleButton;
