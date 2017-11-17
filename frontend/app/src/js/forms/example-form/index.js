// @flow
import { default as ExampleForm } from './ExampleForm';
import reducer from './reducer';

export const type = 'example';

const exampleForm = {
  type,
  component: ExampleForm,
  reducer
};

export default exampleForm;
