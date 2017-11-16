import conquery from '../../../lib/js/conquery';
import {reducer as exampleFormReducer} from "./forms/example-form";

const formReducers = {
  example: exampleFormReducer
};

conquery(formReducers);
