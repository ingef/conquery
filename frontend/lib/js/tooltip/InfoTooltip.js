// @flow

import React                from 'react';
import ReactTooltip         from 'react-tooltip';
import classnames           from 'classnames'

type PropsType = {
  text: string,
  className: string
};

const InfoTooltip = (props: PropsType) => {
  return (
    <span className={classnames("info-tooltip", props.className)}>
      <i
        data-tip={props.text}
        className="fa fa-question-circle-o"
      />
      <ReactTooltip
        place="right"
        type="info"
        effect="solid"
        multiline={true}
      />
    </span>
  );
};

export default InfoTooltip;
