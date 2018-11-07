// @flow

import React                    from 'react';
import PropTypes                from 'prop-types';
import classnames               from 'classnames';
import T                        from 'i18n-react';
import { connect }              from 'react-redux';
import DatePicker               from 'react-datepicker';
import ie                       from 'ie-version';
import moment                   from 'moment';

import { dateTypes }            from '../common/constants';
import { Modal }                from '../modal';
import { InfoTooltip }          from '../tooltip';
import {
  specificDatePattern,
  parseDatePattern
}  from '../common/helpers/dateHelper';

import {
  queryGroupModalClearNode,
  queryGroupModalSetMinDate,
  queryGroupModalSetMaxDate,
  queryGroupModalResetAllDates,
  queryGroupModalSetTouched,
}                               from './actions';

const {
  DATE_FORMAT,
  localizedDateFormat,
} = dateTypes;


const formatDate = (date) =>
  date
    ? date.format(DATE_FORMAT)
    : null;

const getGroupDate = (dateRange, minOrMax) =>
  dateRange && dateRange[minOrMax]
    ? moment(dateRange[minOrMax], DATE_FORMAT)
    : null;

const getTouched = (touched, minOrMax) =>
touched && touched[minOrMax] ? touched[minOrMax] : false

const QueryGroupModal = (props) => {
  if (!props.group) return null;

  const min = 'min';
  const max = 'max';

  const minDate = getGroupDate(props.group.dateRange, min);
  const maxDate = getGroupDate(props.group.dateRange, max);
  const hasActiveDate = !!(minDate || maxDate);

  const minTouched = getTouched(props.group.touched, min);
  const maxTouched = getTouched(props.group.touched, max);

  const { onSetMinDate, onSetMaxDate, onSetTouched } = props;

  const onChangeRawMin = (value) => {
    var { minDate, maxDate } = specificDatePattern(value);

    if (!minDate)
      minDate = parseDatePattern(value);

    onSetMinDate(formatDate(minDate));

    if (maxDate && maxDate.isValid)
      onSetMaxDate(formatDate(maxDate));
  }

  const onChangeRawMax = (value) => {
    var { maxDate } = specificDatePattern(value);

    if (!maxDate)
      maxDate = parseDatePattern(value);

    onSetMaxDate(formatDate(maxDate));
  }

  return (
    <Modal closeModal={props.onCloseModal} doneButton tabIndex={3}>
      <div className="query-group-modal">
        <h3 className="query-group-modal__headline">
          {
            props.group.elements.reduce((parts, concept, i, elements) => (
              [
                ...parts,
                (
                  <span
                    key={i}
                    className="query-group-modal__headline-part"
                    >
                    {concept.label || concept.id}
                  </span>
                ),
                (
                  i !== elements.length - 1 ? <span key={i + '-comma'}>, </span> : ''
                )
              ]
            ), ([
              <span key={-1} className="query-group-modal__headline-part">
                { T.translate('queryGroupModal.headlineStart') }
              </span>
            ]))
          }
        </h3>
        <p className="query-group-modal__explanation">
          { T.translate('queryGroupModal.explanation') }
          <InfoTooltip text={T.translate('tooltip.date.possiblePattern')} />
          {
            hasActiveDate &&
            <span
              className="query-group-modal__reset-all"
              onClick={props.onResetAllDates}
            >
              <i className="fa fa-undo" /> {T.translate('queryNodeEditor.resetAll')}
            </span>
          }
        </p>
        <div className={
            `query-group-modal__dates ${ie.version && ie.version === 11 ? ' ie11' : ''}`
          }>
          <div className="query-group-modal__input-group">
            <label className="input-label" htmlFor="datepicker-min">
              {T.translate('queryGroupModal.dateMinLabel')}
            </label>
            <DatePicker
              id="datepicker-min"
              className={classnames('query-group-modal__datepicker', {
                'query-group-modal__datepicker--has-value': !!minDate,
                'query-group-modal__datepicker--invalid': !minDate && minTouched && hasActiveDate
              })}
              locale="de"
              dateFormat={localizedDateFormat()}
              selected={minDate}
              openToDate={minDate}
              maxDate={moment().add(2, "year")}
              placeholderText={T.translate('queryGroupModal.datePlaceholder')}
              onChange={(date) => onSetMinDate(formatDate(date))}
              onChangeRaw={(event) => onChangeRawMin(event.target.value)}
              onBlur={(event) => onSetTouched(min)}
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
          <div className="query-group-modal__input-group">
            <label className="input-label" htmlFor="datepicker-max">
              {T.translate('queryGroupModal.dateMaxLabel')}
            </label>
            <DatePicker
              id="datepicker-max"
              className={classnames('query-group-modal__datepicker', {
                'query-group-modal__datepicker--has-value': !!maxDate,
                'query-group-modal__datepicker--invalid': !maxDate && maxTouched && hasActiveDate
              })}
              locale="de"
              dateFormat={localizedDateFormat()}
              selected={maxDate}
              openToDate={maxDate}
              maxDate={moment().add(2, "year")}
              placeholderText={T.translate('queryGroupModal.datePlaceholder')}
              onChange={(date) => onSetMaxDate(formatDate(date))}
              onChangeRaw={(event) => onChangeRawMax(event.target.value)}
              onBlur={(event) => onSetTouched(max)}
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
    </Modal>
  );
};

QueryGroupModal.propTypes = {
  group: PropTypes.object,
  andIdx: PropTypes.number,
  onCloseModal: PropTypes.func.isRequired,
  onSetMinDate: PropTypes.func.isRequired,
  onSetMaxDate: PropTypes.func.isRequired,
  onResetAllDates: PropTypes.func.isRequired,
  onSetTouched: PropTypes.func,
};

function findGroup(query, andIdx) {
  if (!query[andIdx]) return null;

  return query[andIdx];
}

const mapStateToProps = (state) => ({
  group: findGroup(
    state.panes.right.tabs.queryEditor.query,
    state.queryGroupModal.andIdx,
  ),
  andIdx: state.queryGroupModal.andIdx,
});

const mapDispatchToProps = (dispatch: any) => ({
  onCloseModal: () => dispatch(queryGroupModalClearNode()),
  onSetMinDate: (andIdx, date) => dispatch(queryGroupModalSetMinDate(andIdx, date)),
  onSetMaxDate: (andIdx, date) => dispatch(queryGroupModalSetMaxDate(andIdx, date)),
  onResetAllDates: (andIdx) => dispatch(queryGroupModalResetAllDates(andIdx)),
  onSetTouched: (andIdx, field, value?) => dispatch(queryGroupModalSetTouched(andIdx, field)),
});

// Used to enhance the dispatchProps with the andIdx
const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...ownProps,
  ...stateProps,
  ...dispatchProps,
  onSetMinDate: (date) =>
    dispatchProps.onSetMinDate(stateProps.andIdx, date),
  onSetMaxDate: (date) =>
    dispatchProps.onSetMaxDate(stateProps.andIdx, date),
  onResetAllDates: () =>
    dispatchProps.onResetAllDates(stateProps.andIdx),
  onSetTouched: (field) =>
    dispatchProps.onSetTouched(stateProps.andIdx, field),
});

export default connect(mapStateToProps, mapDispatchToProps, mergeProps)(QueryGroupModal);
