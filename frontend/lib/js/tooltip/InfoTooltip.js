// @flow

import React                from 'react';
import ReactTooltip         from 'react-tooltip';
import classnames           from 'classnames'

type PropsType = {
  text: string,
  className: string,
  symbol: boolean,
  place?: string
};

const InfoTooltip = (props: PropsType) => {
  return (
    <span className={classnames("info-tooltip", props.className)}>
      <i
        data-tip={props.text}
        className={classnames({'fa fa-question-circle-o': props.symbol})}
      />
      <ReactTooltip
        place={props.place}
        type="info"
        effect="solid"
        multiline={true}
      />
    </span>
  );
};

InfoTooltip.defaultProps = {
  symbol: true,
  place: 'right'
}

export default InfoTooltip;
