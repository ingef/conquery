// @flow

import './exampleForm.sass'

import React                from 'react';
import { Field, reduxForm } from 'redux-form';
import T                    from 'i18n-react';

import {
  EXAMPLE_FORM
} from '../formTypes';

import {
  InputWithLabel
} from '../../editorComponents';

import {
  validateRequired
} from '../validators';

import {
  selectReduxFormState
} from '../stateSelectors';

type PropsType = {
  onSubmit: Function,
};

const ExampleForm = (props: PropsType) => {
  return (
    <form className="example-form">
      <h3>{T.translate('form.exampleForm.headline')}</h3>
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
  getFormState: selectReduxFormState,
  initialValues: {
    text: 0,
  },
  validate: (values) => ({
    text: validateRequired(values.text),
  })
})(ExampleForm);
