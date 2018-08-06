// @flow

import React          from 'react';
import T              from 'i18n-react';
import classnames     from 'classnames';
import DatePicker     from 'react-datepicker';
import moment         from 'moment';
import {
  type FieldPropsType
}                     from 'redux-form';

import { dateTypes }  from '../common/constants';
import {
  parseDatePattern,
  specificDatePattern
}                     from '../common/helpers/dateHelper';

import 'react-datepicker/dist/react-datepicker.css';

const {
  DATE_FORMAT,
  localizedDateFormat,
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
  const minDate = convertToDate(props.input.value.minDate);
  const maxDate = convertToDate(props.input.value.maxDate);

  const onSetDate = value =>
    props.input.onChange(value);

  const onSetMinDate = date =>
    props.input.onChange({
      ...props.input.value,
      'minDate': date
    });

  const onSetMaxDate = date =>
    props.input.onChange({
      ...props.input.value,
      'maxDate': date
    });

  const onChangeRawMin = (value) => {
    var { minDate, maxDate } = specificDatePattern(value);

    if (!minDate)
      minDate = parseDatePattern(value);

    onSetMinDate(formatDate(minDate));

    if (maxDate && maxDate.isValid)
      onSetDate({ minDate: formatDate(minDate), maxDate: formatDate(maxDate) });
  }

  const onChangeRawMax = (value) => {
    var { maxDate } = specificDatePattern(value);

    if (!maxDate)
      maxDate = parseDatePattern(value);

    onSetMaxDate(formatDate(maxDate));
  }

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
              "query-group-modal__datepicker--has-value": !!minDate
            })}
            locale="de"
            dateFormat={localizedDateFormat()}
              selected={minDate}
              openToDate={minDate}
              maxDate={moment().add(2, "year")}
            placeholderText={T.translate('queryGroupModal.datePlaceholder')}
              onChange={(date) => onSetMinDate(formatDate(date))}
              onChangeRaw={(event) => onChangeRawMin(event.target.value)}
              ref={r => {
                if (r && minDate && minDate.isValid) {
                  r.setOpen(false)
                  r.setSelected(minDate)
                }
              }}
              isClearable
              showYearDropdown
              scrollableYearDropdown
              tabIndex={1}
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
              "query-group-modal__datepicker--has-value": !!maxDate
            })}
            locale="de"
            dateFormat={localizedDateFormat()}
              selected={maxDate}
              openToDate={maxDate}
              maxDate={moment().add(2, "year")}
            placeholderText={T.translate('queryGroupModal.datePlaceholder')}
              onChange={(date) => onSetMaxDate(formatDate(date))}
              onChangeRaw={(event) => onChangeRawMax(event.target.value)}
              ref={r => {
                if (r && maxDate && maxDate.isValid) {
                  r.setOpen(false)
                  r.setSelected(maxDate)
                }
              }}
              isClearable
              showYearDropdown
              scrollableYearDropdown
              tabIndex={2}
          />
        </div>
      </div>
    </div>
  );
};

export default DateRangePicker;
