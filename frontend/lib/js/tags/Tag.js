// @flow

import React                             from 'react';
import classnames                        from 'classnames';

type PropsType = {
  label: string,
  isSelected: boolean,
  onClick: () => void
};

const Tag = (props: PropsType) => {
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

export default Tag;
