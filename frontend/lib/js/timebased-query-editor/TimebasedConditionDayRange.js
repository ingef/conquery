// @flow

import React from 'react';
import T                            from 'i18n-react';

import {
  InputWithLabel,
} from '../form-components';


type PropsType = {
  minDays: ?(number | string),
  maxDays: ?(number | string),
  onSetTimebasedConditionMinDays: Function,
  onSetTimebasedConditionMaxDays: Function,
};

const TimebasedConditionDayRange = (props: PropsType) => (
  <div className="timebased-condition__day-range-container">
    <div className="timebased-condition__day-range">
      {
        props.minDays !== undefined && <InputWithLabel
          inputType="number"
          input={{
            value: props.minDays,
            onChange: (value) => props.onSetTimebasedConditionMinDays(value),
          }}
          inputProps={{min: 1}}
          className="input-range__input-with-label"
          placeholder={T.translate('common.timeUnitDays')}
          label={T.translate('timebasedQueryEditor.minDaysLabel')}
          tinyLabel
        />
      }
      {
        props.maxDays !== undefined && <InputWithLabel
          inputType="number"
          input={{
            value: props.maxDays,
            onChange: (value) => props.onSetTimebasedConditionMaxDays(value),
          }}
          inputProps={{min: 1}}
          className="input-range__input-with-label"
          placeholder={T.translate('common.timeUnitDays')}
          label={T.translate('timebasedQueryEditor.maxDaysLabel')}
          tinyLabel
        />
      }
    </div>
  </div>
);

export default TimebasedConditionDayRange;
