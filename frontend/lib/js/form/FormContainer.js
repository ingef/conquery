// @flow

import React                from 'react';
import { connect }          from 'react-redux';

import {
  AVAILABLE_FORMS,
  EXAMPLE_FORM,
}  from './formTypes';

import { ExampleForm }      from '../../../app/src/js/forms/example-form'; // TODO

type PropsType = {
  activeForm: $Keys<typeof AVAILABLE_FORMS>,
};

const FormContainer = (props: PropsType) => {
  let form;

  switch (props.activeForm) {
    case EXAMPLE_FORM:
      form = <ExampleForm />
      break;
    default:
      break;
  }

  return (
    <div className="form-container">
      { form }
    </div>
  );
};

const mapStateToProps = (state) => ({
  activeForm: state.form.activeForm,
});

export default connect(mapStateToProps)(FormContainer);
