// @flow

import React                from 'react';
import classnames           from 'classnames';

type PropsType = {
  onClick: Function,
  className?: string,
};

const CloseIconButton = (props: PropsType) => {
  return (
    <span
      className={classnames(props.className, 'btn--icon', 'btn--icon--padded')}
      onClick={props.onClick}
    >
      <i className="fa fa-close" />
    </span>
  );
};

export default CloseIconButton;
