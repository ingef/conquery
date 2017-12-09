// @flow

import React      from 'react';
import T          from 'i18n-react';
import classnames from 'classnames';
import {
  numberToThreeDigitArray,
}                 from '../common/helpers';


type PropsType = {
  className?: string,
  matchingEntries?: ?number,
};

const TooltipEntries = (props: PropsType) => {
  if (typeof props.matchingEntries === 'undefined' || props.matchingEntries === null) return null;

  return (
    <div className={classnames(
      props.className,
      "tooltip-entries", {
        'tooltip-entries--zero': props.matchingEntries === 0
      },
    )}>
      <div className="tooltip-entries__right-container">
        <i className="tooltip-entries__icon fa fa-bar-chart" />
        <div className="tooltip-entries__info">
          <p className="tooltip-entries__number">
            {
              numberToThreeDigitArray(props.matchingEntries)
              .map((threeDigits, i) => (
                <span key={i} className="tooltip-entries__digits">{threeDigits}</span>
              ))
            }
          </p>
          <p className="tooltip-entries__text">
            {
              T.translate(
                'tooltip.entriesFound',
                { context: props.matchingEntries } // For pluralization
              )
            }
          </p>
        </div>
      </div>
    </div>
  );
};

export default TooltipEntries;
