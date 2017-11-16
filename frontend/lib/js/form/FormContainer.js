// @flow

import React                from 'react';
import { connect }          from 'react-redux';

type PropsType = {
  availableForms: Object,
  activeForm: string,
};

const FormContainer = (props: PropsType) => {
  const form = React.createElement(props.availableForms[props.activeForm].component);
  return (
    <div className="form-container">
      {form}
    </div>
  );
};

const mapStateToProps = (state) => ({
  availableForms: state.form.availableForms,
  activeForm: state.form.activeForm,
});

export default connect(mapStateToProps)(FormContainer);
