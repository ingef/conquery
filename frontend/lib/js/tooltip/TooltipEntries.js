// @flow

import React      from 'react';
import T          from 'i18n-react';
import classnames from 'classnames';
import {
  formatDate,
  numberToThreeDigitArray,
}                 from '../common/helpers';


type PropsType = {
  className?: string,
  matchingEntries?: ?number,
  dateRange?: ?Object,
};

const _renderMatchingEntriesTooltip = (matchingEntries) => (
  <span>
    <i className="tooltip-entries__icon fa fa-bar-chart" />
    <div className="tooltip-entries__info">
      <p className="tooltip-entries__number">
        {
          numberToThreeDigitArray(matchingEntries)
            .map((threeDigits, i) => (
              <span key={i} className="tooltip-entries__digits">{threeDigits}</span>
            ))
        }
      </p>
      <p className="tooltip-entries__text">
        {
          T.translate(
            'tooltip.entriesFound',
            { context: matchingEntries } // For pluralization
          )
        }
      </p>
    </div>
  </span>
);

const _renderConceptDateRangeTooltip = (dateRange) => (
  <span style={{marginLeft: 36}}>
    <i className="tooltip-entries__icon fa fa-calendar" />
    <div className="tooltip-entries__info">
      <div className="tooltip-entries__date-container">
          <p className="tooltip-entries__date">
            {T.translate('tooltip.date.from') + ":"}
          </p>
        <p className="tooltip-entries__date">
          {T.translate('tooltip.date.to') + ":"}
          </p>
      </div>
      <div className="tooltip-entries__date-container">
          <p className="tooltip-entries__date">
            {formatDate(dateRange.lowerEndpoint)}
          </p>
        <p className="tooltip-entries__date">
        {formatDate(dateRange.upperEndpoint)}
          </p>
      </div>
      <p className="tooltip-entries__text">
        {T.translate('tooltip.date.daterange')}
      </p>
    </div>
  </span>
);


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
        {_renderMatchingEntriesTooltip(props.matchingEntries)}
        {
          props.dateRange &&
            _renderConceptDateRangeTooltip(props.dateRange)
        }
      </div>
    </div>
  );
};

export default TooltipEntries;
