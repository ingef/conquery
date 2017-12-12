// @flow

import React                from 'react';
import { connect }          from 'react-redux';

import {
  AVAILABLE_FORMS,
  EXAMPLE_FORM,
}  from './externalFormTypes';

import { ExampleForm }      from './example-form';

type PropsType = {
  activeForm: $Keys<typeof AVAILABLE_FORMS>,
};

const ExternalFormsContainer = (props: PropsType) => {
  let form;

  switch (props.activeForm) {
    case EXAMPLE_FORM:
      form = <ExampleForm />
      break;
    default:
      break;
  }

  return (
    <div className="external-forms-container">
      { form }
    </div>
  );
};

const mapStateToProps = (state) => ({
  activeForm: state.externalForms.activeForm,
});

export default connect(mapStateToProps)(ExternalFormsContainer);
