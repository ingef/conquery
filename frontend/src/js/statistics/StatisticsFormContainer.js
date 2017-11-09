// @flow

import React                from 'react';
import { connect }          from 'react-redux';

import {
  AVAILABLE_FORMS,
  EXAMPLE_FORM,
}  from './statisticsFormTypes';

import { ExampleForm }      from './example-form';

type PropsType = {
  activeForm: $Keys<typeof AVAILABLE_FORMS>,
};

const StatisticsFormContainer = (props: PropsType) => {
  let form;

  switch (props.activeForm) {
    case EXAMPLE_FORM:
      form = <ExampleForm />
      break;
    default:
      break;
  }

  return (
    <div className="statistics-form-container">
      { form }
    </div>
  );
};

const mapStateToProps = (state) => ({
  activeForm: state.statistics.activeForm,
});

export default connect(mapStateToProps)(StatisticsFormContainer);
