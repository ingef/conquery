// @flow

import React       from 'react';
import InfoTooltip from '../tooltip/InfoTooltip';

type PropsType = {
  className: string,
  label: string,
  unit?: string,
  tooltip?: string,
};

const InputRangeHeader = ({ label, unit, className, tooltip }: PropsType) => {
  return (
    <p className={className}>
      { label }
      { unit && ` ( ${unit} )` }
      { tooltip && <InfoTooltip text={tooltip} /> }
    </p>
  );
}

export default InputRangeHeader;
