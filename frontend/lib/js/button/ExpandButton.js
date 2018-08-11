// @flow

import React                from 'react';
import classnames           from 'classnames';
import ReactTooltip         from 'react-tooltip';

type PropsType = {
  onClick: Function,
  className?: string,
  tooltip?: string,
};

const ExpandButton = (props: PropsType) => {
  return (
    <span
      className={classnames(props.className, 'btn--icon', 'btn--icon--padded')}
      onClick={props.onClick}
    >
      <i data-tip data-for="fa-expand" className="fa fa-expand" aria-hidden="true" />
      {
        !!props.tooltip &&
          <ReactTooltip
            id="fa-expand"
            place="top"
            type="info"
            effect="solid"
            multiline={true}
            delayShow={1000}
          >
            <span>{props.tooltip}</span>
          </ReactTooltip>
      }
    </span>
  );
};

export default ExpandButton;
