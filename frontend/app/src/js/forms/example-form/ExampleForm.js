// @flow

import './exampleForm.sass'

import React                from 'react';
import { Field, reduxForm } from 'redux-form';
import { T }                from '../../../../../lib/js/localization';

import {
  InputWithLabel
} from '../../../../../lib/js/form-components';

import {
  FormQueryDropzone
} from '../../../../../lib/js/external-forms/form-query-dropzone';

import {
  FormConceptGroup
} from '../../../../../lib/js/external-forms/form-concept-group';

import {
  validateRequired
} from '../../../../../lib/js/external-forms/validators';

import {
  selectReduxFormState
} from '../../../../../lib/js/external-forms/stateSelectors';

import type {
  ExternalFormPropsType
} from '../../../../../lib/js/external-forms/types';

import { type } from './formType';

type PropsType = {
  onSubmit: Function,
};

const ExampleForm = (props: ExternalFormPropsType | PropsType) => {
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
      <Field
        name="example_query"
        component={FormQueryDropzone}
        props={{
          label: T.translate('externalForms.exampleForm.exampleQuery'),
          dropzoneText: T.translate('externalForms.exampleForm.exampleQueryDropzone'),
        }}
      />
      <Field
        name="example_concepts"
        component={FormConceptGroup}
        props={{
          name: 'example_concepts',
          label: T.translate('externalForms.exampleForm.exampleConcepts'),
          conceptDropzoneText: T.translate('externalForms.exampleForm.exampleConceptDropzone'),
          attributeDropzoneText: T.translate('externalForms.exampleForm.exampleAttributeDropzone'),
          datasetId: props.selectedDatasetId,
          formType: type,
          newValue: { concepts: [] }
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
    example_query: null,
    example_concepts: []
  },
  destroyOnUnmount: false,
  validate: (values) => ({
    text: validateRequired(values.text),
  })
})(ExampleForm);
