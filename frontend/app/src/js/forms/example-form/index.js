// @flow
import { default as ExampleForm }   from './ExampleForm';
import reducer                      from './reducer';
import { type }                     from './formType';

const exampleForm = {
  type,
  component: ExampleForm,
  reducer
};

export default exampleForm;
