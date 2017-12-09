import React, { PropTypes }              from 'react';
import classnames                        from 'classnames';


const Tag = (props) => {
  return (
    <p
      className={classnames(
        "tag", {
          "tag--clickable": !!props.onClick,
          "tag--selected": !!props.isSelected
        }
      )}
      onClick={props.onClick}
    >
      { props.label }
    </p>
  );
}

Tag.propTypes = {
  label: PropTypes.string.isRequired,
  isSelected: PropTypes.bool.isRequired,
  onClick: PropTypes.func,
};

export default Tag;
