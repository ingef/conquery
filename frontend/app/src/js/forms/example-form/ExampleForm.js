// @flow

import './exampleForm.sass'

import React                from 'react';
import { Field, reduxForm } from 'redux-form';
import T                    from 'i18n-react';

import {
  InputWithLabel
} from '../../../../../lib/js/form-components';

import {
  validateRequired
} from '../../../../../lib/js/external-forms/validators';

import {
  selectReduxFormState
} from '../../../../../lib/js/external-forms/stateSelectors';

import { type } from './formType';

type PropsType = {
  onSubmit: Function,
};

const ExampleForm = (props: PropsType) => {
  return (
    <form className="example-form">
      <h3>{T.translate('externalForms.exampleForm.headline')}</h3>
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
  form: type,
  getFormState: selectReduxFormState,
  initialValues: {
    text: '',
  },
  validate: (values) => ({
    text: validateRequired(values.text),
  })
})(ExampleForm);
