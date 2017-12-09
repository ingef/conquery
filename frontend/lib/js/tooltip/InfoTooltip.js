// @flow

import React                from 'react';
import ReactTooltip         from 'react-tooltip';

type PropsType = {
  text: string,
};

const InfoTooltip = (props: PropsType) => {
  return (
    <span className="info-tooltip">
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
