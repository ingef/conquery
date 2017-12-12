// @flow

import React          from 'react';
import T              from 'i18n-react';
import classnames     from 'classnames';
import DatePicker     from 'react-datepicker';
import moment         from 'moment';

import { type FieldPropsType } from 'redux-form';

import { dateTypes }  from '../common/constants';

const {
  DATE_FORMAT,
  LOCALIZED_DATE_FORMAT,
} = dateTypes;

type PropsType = FieldPropsType & {
  label?: string,
  className?: string,
  inputGroupClassName?: string,
  inputGroupElementClassName? : string,
};

const formatDate = (date) =>
  date
    ? date.format(DATE_FORMAT)
    : null;

const convertToDate = date =>
  date
    ? moment(date, DATE_FORMAT)
    : null;

const DateRangePicker = (props: PropsType) => {
  const onChangeMinDate = date =>
    props.input.onChange({
      ...props.input.value,
      'minDate': date,
      'maxDate': props.input.value.maxDate
        ? props.input.value.maxDate
        : null
    });

  const onChangeMaxDate = date =>
    props.input.onChange({
      ...props.input.value,
      'maxDate': date,
      'minDate': props.input.value.minDate
        ? props.input.value.minDate
        : null
    });

  return (
    <div className={props.className}>
      {
        props.label &&
        <span className="input input-label">
          {props.label}
        </span>
      }
      <div className={props.inputGroupClassName}>
        <div className={props.inputGroupElementClassName}>
          <label
            className="input-label"
            htmlFor="datepicker-min"
          >
            {T.translate('externalForms.common.dateMinLabel')}
          </label>
          <DatePicker
            id="datepicker-min"
            className={classnames({
              "query-group-modal__datepicker--has-value": !!props.input.value.minDate
            })}
            locale="de"
            dateFormat={LOCALIZED_DATE_FORMAT}
            selected={convertToDate(props.input.value.minDate)}
            placeholderText={T.translate('queryGroupModal.datePlaceholder')}
            isClearable={true}
            onChange={date => onChangeMinDate(formatDate(date))}
            showYearDropdown={true}
            scrollableYearDropdown={true}
          />
        </div>
        <div className={props.inputGroupElementClassName}>
          <label
            className="input-label"
            htmlFor="datepicker-max"
          >
            {T.translate('externalForms.common.dateMaxLabel')}
          </label>
          <DatePicker
            id="datepicker-max"
            className={classnames({
              "query-group-modal__datepicker--has-value": !!props.input.value.maxDate
            })}
            locale="de"
            dateFormat={LOCALIZED_DATE_FORMAT}
            selected={convertToDate(props.input.value.maxDate)}
            placeholderText={T.translate('queryGroupModal.datePlaceholder')}
            isClearable={true}
            onChange={date => onChangeMaxDate(formatDate(date))}
            showYearDropdown={true}
            scrollableYearDropdown={true}
          />
        </div>
      </div>
    </div>
  );
};

export default DateRangePicker;
