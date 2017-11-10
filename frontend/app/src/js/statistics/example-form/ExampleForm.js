// @flow

import './exampleForm.sass'

import React                from 'react';
import { Field, reduxForm } from 'redux-form';
import T                    from 'i18n-react';

import {
  EXAMPLE_FORM
} from '../../../../../lib/js/statistics/statisticsFormTypes';

import {
  InputWithLabel
} from '../../../../../lib/js/editorComponents';

import {
  validateRequired
} from '../../../../../lib/js/statistics/validators';

type PropsType = {
  onSubmit: Function,
};

const ExampleForm = (props: PropsType) => {
  return (
    <form className="example-form">
      <h3>{T.translate('statistics.exampleForm.headline')}</h3>
      <Field
        name="text"
        component={InputWithLabel}
        props={{
          inputType: "text",
          label: T.translate('common.title'),
        }}
      />
    </form>
  );
};

export default reduxForm({
  form: EXAMPLE_FORM,
  getFormState: (state) => state.statistics.form,
  initialValues: {
    text: 0,
  },
  validate: (values) => ({
    text: validateRequired(values.text),
  })
})(ExampleForm);
