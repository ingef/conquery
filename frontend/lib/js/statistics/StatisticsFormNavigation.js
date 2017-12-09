// @flow

import React                 from 'react';
import T                     from 'i18n-react';
import type { Dispatch }     from 'redux-thunk';
import { connect }           from 'react-redux';

import { InputSelect }       from '../form';

import { setStatisticsForm } from './actions';
import { AVAILABLE_FORMS }   from './statisticsFormTypes';

type PropsType = {
  activeForm: $Keys<typeof AVAILABLE_FORMS>,
  onItemClick: Function,
};

const StatisticsFormNavigation = (props: PropsType) => {
  const options = Object
    .keys(AVAILABLE_FORMS)
    .map(formType => ({
      label: formType,
      value: formType
    }));

  return (
    <div className="statistics-form-navigation">
      <InputSelect
        label={T.translate('statistics.forms')}
        options={options}
        input={{
          value: props.activeForm,
          onChange: (value) => props.onItemClick(value)
        }}
        selectProps={{
          clearable: false,
          autosize: true,
          searchable: false,
        }}
      />
    </div>
  );
};

const mapStateToProps = (state) => ({
  activeForm: state.statistics.activeForm,
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onItemClick: (form) => dispatch(setStatisticsForm(form)),
});

export default connect(mapStateToProps, mapDispatchToProps)(StatisticsFormNavigation);
