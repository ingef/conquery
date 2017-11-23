// @flow
import { default as ExampleForm } from './ExampleForm';
import reducer from './reducer';
import { formType } from './formType';

const exampleForm = {
  formType,
  component: ExampleForm,
  reducer
};

export default exampleForm;
